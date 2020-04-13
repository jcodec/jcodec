package org.jcodec.containers.mps

import org.jcodec.common.IntObjectMap
import org.jcodec.common.Preconditions
import org.jcodec.common.and
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.demuxer.DemuxerProbe
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 *
 * MPEG TS demuxer
 *
 * @author The JCodec project
 */
class MTSDemuxer(private val channel: SeekableByteChannel) {
    private val _programs: MutableMap<Int, ProgramChannel> = HashMap()

    init {
        for (pid in findPrograms(channel)) {
            _programs[pid] = ProgramChannel(this)
        }
        channel.setPosition(0)
    }

    fun getPrograms(): Set<Int> = _programs.keys

    @Throws(IOException::class)
    fun findPrograms(src: SeekableByteChannel): Set<Int> {
        val rem = src.position()
        val guids: MutableSet<Int> = HashSet()
        var i = 0
        while (guids.size == 0 || i < guids.size * 500) {
            val pkt = readPacket(src) ?: break
            if (pkt.payload == null) {
                i++
                continue
            }
            val payload = pkt.payload
            if (!guids.contains(pkt.pid) && payload!!.duplicate().int and 0xff.inv() == 0x100) {
                guids.add(pkt.pid)
            }
            i++
        }
        src.setPosition(rem)
        return guids
    }

    fun getProgram(pid: Int): ReadableByteChannel? {
        return _programs[pid]
    }

    //In Javascript you cannot access a field from the outer type. You should define a variable var that=this outside your function definition and use the property of this object.
    //In Javascript you cannot call methods or fields from the outer type. You should define a variable var that=this outside your function definition and call the methods on this object
    private class ProgramChannel(private val demuxer: MTSDemuxer) : ReadableByteChannel {
        private val data: MutableList<ByteBuffer?>
        private var closed = false
        override fun isOpen(): Boolean {
            return !closed && demuxer.channel.isOpen
        }

        @Throws(IOException::class)
        override fun close() {
            closed = true
            data.clear()
        }

        @Throws(IOException::class)
        override fun read(dst: ByteBuffer): Int {
            var bytesRead = 0
            while (dst.hasRemaining()) {
                while (data.size == 0) {
                    if (!demuxer.readAndDispatchNextTSPacket()) return if (bytesRead > 0) bytesRead else -1
                }
                val first = data[0]
                val toRead = Math.min(dst.remaining(), first!!.remaining())
                dst.put(NIOUtils.read(first, toRead))
                if (!first.hasRemaining()) data.removeAt(0)
                bytesRead += toRead
            }
            return bytesRead
        }

        fun storePacket(pkt: MTSPacket) {
            if (closed) return
            data.add(pkt.payload)
        }

        init {
            data = ArrayList()
        }
    }

    @Throws(IOException::class)
    private fun readAndDispatchNextTSPacket(): Boolean {
        val pkt = readPacket(channel) ?: return false
        val program = _programs[pkt.pid]
        program?.storePacket(pkt)
        return true
    }

    class MTSPacket(var pid: Int, var payloadStart: Boolean, var payload: ByteBuffer?)

    companion object {
        @Throws(IOException::class)
        fun readPacket(channel: ReadableByteChannel?): MTSPacket? {
            val buffer = ByteBuffer.allocate(188)
            if (NIOUtils.readFromChannel(channel, buffer) != 188) return null
            buffer.flip()
            return parsePacket(buffer)
        }

        fun parsePacket(buffer: ByteBuffer): MTSPacket {
            val marker: Int = buffer.get() and 0xff
            Preconditions.checkState(0x47 == marker)
            val guidFlags = buffer.short.toInt()
            val guid = guidFlags and 0x1fff
            val payloadStart = guidFlags shr 14 and 0x1
            val b0: Int = buffer.get() and 0xff
            val counter = b0 and 0xf
            if (b0 and 0x20 != 0) {
                var taken = 0
                taken = (buffer.get() and 0xff) + 1
                NIOUtils.skip(buffer, taken - 1)
            }
            return MTSPacket(guid, payloadStart == 1, if (b0 and 0x10 != 0) buffer else null)
        }

        @JvmField
        val PROBE: DemuxerProbe = object : DemuxerProbe {
            override fun probe(b_: ByteBuffer): Int {
                val b = b_.duplicate()
                val streams = IntObjectMap<MutableList<ByteBuffer?>>()
                while (true) {
                    try {
                        val sub = NIOUtils.read(b, 188)
                        if (sub.remaining() < 188) break
                        val tsPkt = parsePacket(sub) ?: break
                        var data = streams[tsPkt.pid]
                        if (data == null) {
                            data = ArrayList()
                            streams.put(tsPkt.pid, data)
                        }
                        if (tsPkt.payload != null) data.add(tsPkt.payload)
                    } catch (t: Throwable) {
                        break
                    }
                }
                var maxScore = 0
                val keys = streams.keys()
                for (i in keys) {
                    val packets: List<ByteBuffer?> = streams[i]!!
                    val b1 = NIOUtils.combineBuffers(packets)
                    val score = MPSDemuxer.PROBE.probe(b1)
                    if (score > maxScore) {
                        maxScore = score + if (packets.size > 20) 50 else 0
                    }
                }
                return maxScore
            }
        }
    }
}