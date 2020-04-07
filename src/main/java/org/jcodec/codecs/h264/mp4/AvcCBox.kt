package org.jcodec.codecs.h264.mp4

import org.jcodec.common.Preconditions
import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Header
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
class AvcCBox(header: Header) : Box(header) {
    var profile = 0
        private set
    var profileCompat = 0
        private set
    var level = 0
        private set

    //    public void toNAL(ByteBuffer codecPrivate) {
    var nalLengthSize = 0
        private set
    private var spsList: MutableList<ByteBuffer>
    private var ppsList: MutableList<ByteBuffer>
    override fun parse(input: ByteBuffer) {
        NIOUtils.skip(input, 1)
        profile = input.get().toInt() and 0xff
        profileCompat = input.get().toInt() and 0xff
        level = input.get().toInt() and 0xff
        val flags: Int = input.get().toInt() and 0xff
        nalLengthSize = (flags and 0x03) + 1
        val nSPS: Int = input.get().toInt() and 0x1f // 3 bits reserved + 5 bits number of
        // sps
        for (i in 0 until nSPS) {
            val spsSize = input.short.toInt()
            Preconditions.checkState(0x27 == input.get().toInt() and 0x3f)
            spsList.add(NIOUtils.read(input, spsSize - 1))
        }
        val nPPS: Int = input.get().toInt() and 0xff
        for (i in 0 until nPPS) {
            val ppsSize = input.short.toInt()
            Preconditions.checkState(0x28 == input.get().toInt() and 0x3f)
            ppsList.add(NIOUtils.read(input, ppsSize - 1))
        }
    }

    public override fun doWrite(out: ByteBuffer) {
        out.put(0x1.toByte()) // version
        out.put(profile.toByte())
        out.put(profileCompat.toByte())
        out.put(level.toByte())
        out.put(0xff.toByte())
        out.put((spsList.size or 0xe0).toByte())
        for (sps in spsList) {
            out.putShort((sps.remaining() + 1).toShort())
            out.put(0x67.toByte())
            NIOUtils.write(out, sps)
        }
        out.put(ppsList.size.toByte())
        for (pps in ppsList) {
            out.putShort((pps.remaining() + 1).toShort())
            out.put(0x68.toByte())
            NIOUtils.write(out, pps)
        }
    }

    override fun estimateSize(): Int {
        var sz = 17
        for (sps in spsList) {
            sz += 3 + sps.remaining()
        }
        for (pps in ppsList) {
            sz += 3 + pps.remaining()
        }
        return sz
    }

    fun getSpsList(): List<ByteBuffer> {
        return spsList
    }

    fun getPpsList(): List<ByteBuffer> {
        return ppsList
    }

    //        H264Utils.toNAL(codecPrivate, getSpsList(), getPpsList());
    //    }
    //
    //    public ByteBuffer toNAL() {
    //        ByteBuffer bb = ByteBuffer.allocate(2048);
    //        H264Utils.toNAL(bb, getSpsList(), getPpsList());
    //        bb.flip();
    //        return bb;
    //    }
    //
    //    public static AvcCBox fromNAL(ByteBuffer codecPrivate) {
    //        List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
    //        List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
    //
    //        ByteBuffer dup = codecPrivate.duplicate();
    //
    //        ByteBuffer buf;
    //        SeqParameterSet sps = null;
    //        while ((buf = H264Utils.nextNALUnit(dup)) != null) {
    //            NALUnit nu = NALUnit.read(buf);
    //
    //            H264Utils.unescapeNAL(buf);
    //
    //            if (nu.type == NALUnitType.PPS) {
    //                ppsList.add(buf);
    //            } else if (nu.type == NALUnitType.SPS) {
    //                spsList.add(buf);
    //                sps = SeqParameterSet.read(buf.duplicate());
    //            }
    //        }
    //        if (spsList.size() == 0 || ppsList.size() == 0)
    //            return null;
    //        return new AvcCBox(sps.profile_idc, 0, sps.level_idc, spsList, ppsList);
    //    }
    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "avcC"
        }

        @JvmStatic
        fun parseAvcCBox(buf: ByteBuffer): AvcCBox {
            val avcCBox = AvcCBox(Header(fourcc()))
            avcCBox.parse(buf)
            return avcCBox
        }

        fun createEmpty(): AvcCBox {
            return AvcCBox(Header(fourcc()))
        }

        @JvmStatic
        fun createAvcCBox(profile: Int, profileCompat: Int, level: Int, nalLengthSize: Int,
                          spsList: List<ByteBuffer>, ppsList: List<ByteBuffer>): AvcCBox {
            val avcc = AvcCBox(Header(fourcc()))
            avcc.profile = profile
            avcc.profileCompat = profileCompat
            avcc.level = level
            avcc.nalLengthSize = nalLengthSize
            avcc.spsList = spsList.toMutableList()
            avcc.ppsList = ppsList.toMutableList()
            return avcc
        }
    }

    init {
        spsList = ArrayList()
        ppsList = ArrayList()
    }
}