package org.jcodec.codecs.h264

import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart1
import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart2
import org.jcodec.codecs.h264.io.model.*
import org.jcodec.codecs.h264.io.model.NALUnit.Companion.read
import org.jcodec.codecs.h264.io.model.SeqParameterSet.Companion.getPicHeightInMbs
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter.write
import org.jcodec.codecs.h264.mp4.AvcCBox
import org.jcodec.codecs.h264.mp4.AvcCBox.Companion.createAvcCBox
import org.jcodec.codecs.h264.mp4.AvcCBox.Companion.parseAvcCBox
import org.jcodec.common.IntArrayList
import org.jcodec.common.and
import org.jcodec.common.io.*
import org.jcodec.common.model.Size
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.SampleEntry
import org.jcodec.containers.mp4.boxes.VideoSampleEntry
import org.jcodec.containers.mp4.boxes.VideoSampleEntry.Companion.videoSampleEntry
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object H264Utils {
    @JvmStatic
    fun nextNALUnit(buf: ByteBuffer): ByteBuffer? {
        skipToNALUnit(buf)
        return if (buf.hasArray()) gotoNALUnitWithArray(buf) else gotoNALUnit(buf)
    }

    fun skipToNALUnit(buf: ByteBuffer) {
        if (!buf.hasRemaining()) return
        var `val` = -0x1
        while (buf.hasRemaining()) {
            `val` = `val` shl 8
            `val` = `val` or (buf.get().toInt() and 0xff)
            if (`val` and 0xffffff == 1) {
                buf.position(buf.position())
                break
            }
        }
    }

    /**
     * Finds next Nth H.264 bitstream NAL unit (0x00000001) and returns the data
     * that preceeds it as a ByteBuffer slice
     *
     * Segment byte order is always little endian
     *
     * TODO: emulation prevention
     *
     * @param buf
     * @return
     */
    @JvmStatic
    fun gotoNALUnit(buf: ByteBuffer): ByteBuffer? {
        if (!buf.hasRemaining()) return null
        val from = buf.position()
        val result = buf.slice()
        result.order(ByteOrder.BIG_ENDIAN)
        var `val` = -0x1
        while (buf.hasRemaining()) {
            `val` = `val` shl 8
            `val` = `val` or (buf.get() and 0xff)
            if (`val` and 0xffffff == 1) {
                buf.position(buf.position() - if (`val` == 1) 4 else 3)
                result.limit(buf.position() - from)
                break
            }
        }
        return result
    }

    /**
     * Finds next Nth H.264 bitstream NAL unit (0x00000001) and returns the data
     * that preceeds it as a ByteBuffer slice
     *
     * Segment byte order is always little endian
     *
     * @param buf
     * @return data
     */
    @JvmStatic
    fun gotoNALUnitWithArray(buf: ByteBuffer): ByteBuffer? {
        if (!buf.hasRemaining()) return null
        val from = buf.position()
        val result = buf.slice()
        result.order(ByteOrder.BIG_ENDIAN)
        val arr = buf.array()
        var pos = from + buf.arrayOffset()
        val posFrom = pos
        val lim = buf.limit() + buf.arrayOffset()
        while (pos < lim) {
            var b = arr[pos]
            if (b and 254 == 0) {
                while (b.toInt() == 0 && ++pos < lim) b = arr[pos]
                if (b.toInt() == 1) {
                    if (pos - posFrom >= 2 && arr[pos - 1] == 0.toByte() && arr[pos - 2] == 0.toByte()) {
                        val lenSize = if (pos - posFrom >= 3 && arr[pos - 3] == 0.toByte()) 4 else 3
                        buf.position(pos + 1 - buf.arrayOffset() - lenSize)
                        result.limit(buf.position() - from)
                        return result
                    }
                }
            }
            pos += 3
        }
        buf.position(buf.limit())
        return result
    }

    fun unescapeNAL(_buf: ByteBuffer) {
        if (_buf.remaining() < 2) return
        val _in = _buf.duplicate()
        val out = _buf.duplicate()
        var p1 = _in.get()
        out.put(p1)
        var p2 = _in.get()
        out.put(p2)
        while (_in.hasRemaining()) {
            val b = _in.get()
            if (p1.toInt() != 0 || p2.toInt() != 0 || b.toInt() != 3) out.put(b)
            p1 = p2
            p2 = b
        }
        _buf.limit(out.position())
    }

    @JvmStatic
    fun escapeNALinplace(src: ByteBuffer) {
        val loc = searchEscapeLocations(src)
        val old = src.limit()
        src.limit(src.limit() + loc.size)
        var newPos = src.limit() - 1
        var oldPos = old - 1
        var locIdx = loc.size - 1
        while (newPos >= src
                        .position()) {
            src.put(newPos, src[oldPos])
            if (locIdx >= 0 && loc[locIdx] == oldPos) {
                newPos--
                src.put(newPos, 3.toByte())
                locIdx--
            }
            newPos--
            oldPos--
        }
    }

    private fun searchEscapeLocations(src: ByteBuffer): IntArray {
        val points = IntArrayList.createIntArrayList()
        val search = src.duplicate()
        var p = search.short.toInt()
        while (search.hasRemaining()) {
            val b = search.get()
            if (p == 0 && b and 3.inv() == 0) {
                points.add(search.position() - 1)
                p = 3
            }
            p = p shl 8 and 0xffff
            p = p or (b and 0xff)
        }
        return points.toArray()
    }

    @JvmStatic
    fun escapeNAL(src: ByteBuffer, dst: ByteBuffer) {
        var p1 = src.get()
        var p2 = src.get()
        dst.put(p1)
        dst.put(p2)
        while (src.hasRemaining()) {
            val b = src.get()
            if (p1.toInt() == 0 && p2.toInt() == 0 && b and 0xff <= 3) {
                dst.put(3.toByte())
                p1 = p2
                p2 = 3
            }
            dst.put(b)
            p1 = p2
            p2 = b
        }
    }

    @JvmStatic
    fun splitMOVPacket(buf: ByteBuffer, avcC: AvcCBox): List<ByteBuffer> {
        val result: MutableList<ByteBuffer> = ArrayList()
        val nls = avcC.nalLengthSize
        val dup = buf.duplicate()
        while (dup.remaining() >= nls) {
            val len = readLen(dup, nls)
            if (len == 0) break
            result.add(NIOUtils.read(dup, len))
        }
        return result
    }

    private fun readLen(dup: ByteBuffer, nls: Int): Int {
        return when (nls) {
            1 -> dup.get() and 0xff
            2 -> dup.short and 0xffff
            3 -> dup.short and 0xffff shl 8 or (dup.get() and 0xff)
            4 -> dup.int
            else -> throw IllegalArgumentException("NAL Unit length size can not be $nls")
        }
    }

    /**
     * Encodes AVC frame in ISO BMF format. Takes Annex B format.
     *
     * Scans the packet for each NAL Unit starting with 00 00 00 01 and replaces
     * this 4 byte sequence with 4 byte integer representing this NAL unit
     * length.
     *
     * @param avcFrame
     * AVC frame encoded in Annex B NAL unit format
     */
    fun encodeMOVPacketInplace(avcFrame: ByteBuffer) {
        val dup = avcFrame.duplicate()
        val d1 = avcFrame.duplicate()
        var tot = d1.position()
        while (true) {
            val buf = nextNALUnit(dup) ?: break
            d1.position(tot)
            d1.putInt(buf.remaining())
            tot += buf.remaining() + 4
        }
    }

    /**
     * Encodes AVC frame in ISO BMF format. Takes Annex B format.
     *
     * Scans the packet for each NAL Unit starting with 00 00 00 01 and replaces
     * this 4 byte sequence with 4 byte integer representing this NAL unit
     * length.
     *
     * @param avcFrame
     * AVC frame encoded in Annex B NAL unit format
     */
    fun encodeMOVPacket(avcFrame: ByteBuffer): ByteBuffer {
        val dup = avcFrame.duplicate()
        val list: MutableList<ByteBuffer> = ArrayList()
        var buf: ByteBuffer?
        var totalLen = 0
        while (nextNALUnit(dup).also { buf = it } != null) {
            list.add(buf!!)
            totalLen += buf!!.remaining()
        }
        val result = ByteBuffer.allocate(list.size * 4 + totalLen)
        for (byteBuffer in list) {
            result.putInt(byteBuffer.remaining())
            result.put(byteBuffer)
        }
        result.flip()
        return result
    }

    /**
     * Decodes AVC packet in ISO BMF format into Annex B format.
     *
     * Replaces NAL unit size integers with 00 00 00 01 start codes. If the
     * space allows the transformation is done inplace.
     *
     * @param result
     */
    @JvmStatic
    fun decodeMOVPacket(result: ByteBuffer, avcC: AvcCBox): ByteBuffer {
        if (avcC.nalLengthSize == 4) {
            decodeMOVPacketInplace(result, avcC)
            return result
        }
        return decodeMOVPacketNewBuf(result, avcC)
    }

    fun decodeMOVPacketNewBuf(result: ByteBuffer, avcC: AvcCBox): ByteBuffer {
        return joinNALUnits(splitMOVPacket(result, avcC))
    }

    /**
     * Decodes AVC packet in ISO BMF format into Annex B format.
     *
     * Inplace replaces NAL unit size integers with 00 00 00 01 start codes.
     *
     * @param result
     */
    fun decodeMOVPacketInplace(result: ByteBuffer, avcC: AvcCBox) {
        require(avcC.nalLengthSize == 4) { "Can only inplace decode AVC MOV packet with nal_length_size = 4." }
        val dup = result.duplicate()
        while (dup.remaining() >= 4) {
            val size = dup.int
            dup.position(dup.position() - 4)
            dup.putInt(1)
            dup.position(dup.position() + size)
        }
    }

    /**
     * Wipes AVC parameter sets ( SPS/PPS ) from the packet
     *
     * @param in
     * AVC frame encoded in Annex B NAL unit format
     * @param out
     * Buffer where packet without PS will be put
     * @param spsList
     * Storage for leading SPS structures ( can be null, then all
     * leading SPSs are discarded ).
     * @param ppsList
     * Storage for leading PPS structures ( can be null, then all
     * leading PPSs are discarded ).
     */
    fun wipePS(_in: ByteBuffer, out: ByteBuffer?, spsList: MutableList<ByteBuffer?>?, ppsList: MutableList<ByteBuffer?>?) {
        val dup = _in.duplicate()
        while (dup.hasRemaining()) {
            val buf = nextNALUnit(dup) ?: break
            val nu = read(buf.duplicate())
            if (nu.type == NALUnitType.PPS) {
                ppsList?.add(NIOUtils.duplicate(buf))
            } else if (nu.type == NALUnitType.SPS) {
                spsList?.add(NIOUtils.duplicate(buf))
            } else if (out != null) {
                out.putInt(1)
                out.put(buf)
            }
        }
        out?.flip()
    }

    /**
     * Wipes AVC parameter sets ( SPS/PPS ) from the packet ( inplace operation
     * )
     *
     * @param in
     * AVC frame encoded in Annex B NAL unit format
     * @param spsList
     * Storage for leading SPS structures ( can be null, then all
     * leading SPSs are discarded ).
     * @param ppsList
     * Storage for leading PPS structures ( can be null, then all
     * leading PPSs are discarded ).
     */
    fun wipePSinplace(_in: ByteBuffer, spsList: MutableCollection<ByteBuffer>?, ppsList: MutableCollection<ByteBuffer>?) {
        val dup = _in.duplicate()
        while (dup.hasRemaining()) {
            val buf = nextNALUnit(dup) ?: break
            val nu = read(buf)
            if (nu.type == NALUnitType.PPS) {
                ppsList?.add(NIOUtils.duplicate(buf))
                _in.position(dup.position())
            } else if (nu.type == NALUnitType.SPS) {
                spsList?.add(NIOUtils.duplicate(buf))
                _in.position(dup.position())
            } else if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) break
        }
    }

    fun createAvcC(sps: SeqParameterSet, pps: PictureParameterSet, nalLengthSize: Int): AvcCBox {
        val serialSps = ByteBuffer.allocate(512)
        sps.write(serialSps)
        serialSps.flip()
        escapeNALinplace(serialSps)
        val serialPps = ByteBuffer.allocate(512)
        pps.write(serialPps)
        serialPps.flip()
        escapeNALinplace(serialPps)
        return createAvcCBox(sps.profileIdc, 0, sps.levelIdc, nalLengthSize, Arrays.asList(serialSps),
                Arrays.asList(serialPps))
    }

    fun createAvcCFromList(initSPS: List<SeqParameterSet>, initPPS: List<PictureParameterSet>,
                           nalLengthSize: Int): AvcCBox {
        val serialSps = saveSPS(initSPS)
        val serialPps = savePPS(initPPS)
        val sps = initSPS[0]
        return createAvcCBox(sps.profileIdc, 0, sps.levelIdc, nalLengthSize, serialSps, serialPps)
    }

    /**
     * @param initPPS
     * @return
     */
    fun savePPS(initPPS: List<PictureParameterSet>): List<ByteBuffer> {
        val serialPps: MutableList<ByteBuffer> = ArrayList()
        for (pps in initPPS) {
            val bb1 = ByteBuffer.allocate(512)
            pps.write(bb1)
            bb1.flip()
            escapeNALinplace(bb1)
            serialPps.add(bb1)
        }
        return serialPps
    }

    /**
     * @param initSPS
     * @return
     */
    fun saveSPS(initSPS: List<SeqParameterSet>): List<ByteBuffer> {
        val serialSps: MutableList<ByteBuffer> = ArrayList()
        for (sps in initSPS) {
            val bb1 = ByteBuffer.allocate(512)
            sps.write(bb1)
            bb1.flip()
            escapeNALinplace(bb1)
            serialSps.add(bb1)
        }
        return serialSps
    }

    /**
     * Creates a MP4 sample entry given AVC/H.264 codec private.
     *
     * @param codecPrivate
     * Array containing AnnexB delimited (00 00 00 01) SPS/PPS NAL
     * units.
     * @return MP4 sample entry
     */
    fun createMOVSampleEntryFromBytes(codecPrivate: ByteBuffer): SampleEntry {
        val rawSPS = getRawSPS(codecPrivate.duplicate())
        val rawPPS = getRawPPS(codecPrivate.duplicate())
        return createMOVSampleEntryFromSpsPpsList(rawSPS, rawPPS, 4)
    }

    fun createMOVSampleEntryFromSpsPpsList(spsList: List<ByteBuffer>, ppsList: List<ByteBuffer>?,
                                           nalLengthSize: Int): SampleEntry {
        val avcC = createAvcCFromPS(spsList, ppsList, nalLengthSize)
        return createMOVSampleEntryFromAvcC(avcC)
    }

    /**
     * Creates a MP4 sample entry given AVC/H.264 codec private.
     *
     * @param codecPrivate
     * Array containing AnnexB delimited (00 00 00 01) SPS/PPS NAL
     * units.
     * @return MP4 sample entry
     */
    fun createAvcCFromBytes(codecPrivate: ByteBuffer): AvcCBox {
        val rawSPS = getRawSPS(codecPrivate.duplicate())
        val rawPPS = getRawPPS(codecPrivate.duplicate())
        return createAvcCFromPS(rawSPS, rawPPS, 4)
    }

    fun createAvcCFromPS(spsList: List<ByteBuffer>, ppsList: List<ByteBuffer>?, nalLengthSize: Int): AvcCBox {
        val sps = readSPS(NIOUtils.duplicate(spsList[0]))
        return createAvcCBox(sps.profileIdc, 0, sps.levelIdc, nalLengthSize, spsList, ppsList!!)
    }

    fun createMOVSampleEntryFromAvcC(avcC: AvcCBox): SampleEntry {
        val sps = SeqParameterSet.read(avcC.getSpsList()[0].duplicate())
        val codedWidth = sps.picWidthInMbsMinus1 + 1 shl 4
        val codedHeight = getPicHeightInMbs(sps) shl 4
        val se: SampleEntry = videoSampleEntry("avc1", getPicSize(sps), "JCodec")
        se.add(avcC)
        return se
    }

    fun createMOVSampleEntryFromSpsPps(initSPS: SeqParameterSet, initPPS: PictureParameterSet,
                                       nalLengthSize: Int): SampleEntry {
        val bb1 = ByteBuffer.allocate(512)
        val bb2 = ByteBuffer.allocate(512)
        initSPS.write(bb1)
        initPPS.write(bb2)
        bb1.flip()
        bb2.flip()
        return createMOVSampleEntryFromBuffer(bb1, bb2, nalLengthSize)
    }

    fun createMOVSampleEntryFromBuffer(sps: ByteBuffer, pps: ByteBuffer, nalLengthSize: Int): SampleEntry {
        return createMOVSampleEntryFromSpsPpsList(Arrays.asList(*arrayOf(sps)),
                Arrays.asList(*arrayOf(pps)), nalLengthSize)
    }

    fun iFrame(_data: ByteBuffer): Boolean {
        val data = _data.duplicate()
        var segment: ByteBuffer
        while (nextNALUnit(data).also { segment = it!! } != null) {
            val type = read(segment).type
            if (type == NALUnitType.IDR_SLICE || type == NALUnitType.NON_IDR_SLICE) {
                unescapeNAL(segment)
                val reader = BitReader.createBitReader(segment)
                val part1 = readPart1(reader)
                return part1.sliceType == SliceType.I
            }
        }
        return false
    }

    @JvmStatic
    fun isByteBufferIDRSlice(_data: ByteBuffer): Boolean {
        val data = _data.duplicate()
        var segment: ByteBuffer?
        while (nextNALUnit(data).also { segment = it } != null) {
            if (read(segment!!).type == NALUnitType.IDR_SLICE) return true
        }
        return false
    }

    @JvmStatic
    fun idrSlice(_data: List<ByteBuffer>): Boolean {
        for (segment in _data) {
            if (read(segment.duplicate()).type == NALUnitType.IDR_SLICE) return true
        }
        return false
    }

    @Throws(IOException::class)
    fun saveRawFrame(data: ByteBuffer, avcC: AvcCBox, f: File?) {
        val raw: SeekableByteChannel = NIOUtils.writableChannel(f)
        saveStreamParams(avcC, raw)
        raw.write(data.duplicate())
        raw.close()
    }

    @Throws(IOException::class)
    fun saveStreamParams(avcC: AvcCBox, raw: SeekableByteChannel) {
        val bb = ByteBuffer.allocate(1024)
        for (byteBuffer in avcC.getSpsList()) {
            raw.write(ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1, 0x67)))
            escapeNAL(byteBuffer.duplicate(), bb)
            bb.flip()
            raw.write(bb)
            bb.clear()
        }
        for (byteBuffer in avcC.getPpsList()) {
            raw.write(ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1, 0x68)))
            escapeNAL(byteBuffer.duplicate(), bb)
            bb.flip()
            raw.write(bb)
            bb.clear()
        }
    }

    @JvmStatic
    fun splitFrame(frame: ByteBuffer): List<ByteBuffer> {
        val result = ArrayList<ByteBuffer>()
        var segment: ByteBuffer?
        while (nextNALUnit(frame).also { segment = it } != null) {
            result.add(segment!!)
        }
        return result
    }

    /**
     * Joins buffers containing individual NAL units into a single AnnexB
     * delimited buffer. Each NAL unit will be separated with 00 00 00 01
     * markers. Allocates a new byte buffer and writes data into it.
     *
     * @param nalUnits
     * @param out
     */
    fun joinNALUnits(nalUnits: List<ByteBuffer>): ByteBuffer {
        var size = 0
        for (nal in nalUnits) {
            size += 4 + nal.remaining()
        }
        val allocate = ByteBuffer.allocate(size)
        joinNALUnitsToBuffer(nalUnits, allocate)
        allocate.flip()
        return allocate
    }

    /**
     * Joins buffers containing individual NAL units into a single AnnexB
     * delimited buffer. Each NAL unit will be separated with 00 00 00 01
     * markers.
     *
     * @param nalUnits
     * @param out
     */
    @JvmStatic
    fun joinNALUnitsToBuffer(nalUnits: List<ByteBuffer>, out: ByteBuffer) {
        for (nal in nalUnits) {
            out.putInt(1)
            out.put(nal.duplicate())
        }
    }

    fun getAvcCData(avcC: AvcCBox): ByteBuffer {
        val bb = ByteBuffer.allocate(2048)
        avcC.doWrite(bb)
        bb.flip()
        return bb
    }

    fun parseAVCC(vse: VideoSampleEntry?): AvcCBox? {
        val lb = findFirst(vse, "avcC")
        if (lb is AvcCBox) return lb else if (lb != null) {
            return parseAVCCFromBuffer((lb as LeafBox).getData().duplicate())
        }
        return null
    }

    fun saveCodecPrivate(spsList: List<ByteBuffer>, ppsList: List<ByteBuffer>): ByteBuffer {
        var totalCodecPrivateSize = 0
        for (byteBuffer in spsList) {
            totalCodecPrivateSize += byteBuffer.remaining() + 5
        }
        for (byteBuffer in ppsList) {
            totalCodecPrivateSize += byteBuffer.remaining() + 5
        }
        val bb = ByteBuffer.allocate(totalCodecPrivateSize)
        for (byteBuffer in spsList) {
            bb.putInt(1)
            bb.put(0x67.toByte())
            bb.put(byteBuffer.duplicate())
        }
        for (byteBuffer in ppsList) {
            bb.putInt(1)
            bb.put(0x68.toByte())
            bb.put(byteBuffer.duplicate())
        }
        bb.flip()
        return bb
    }

    @JvmStatic
    fun avcCToAnnexB(avcC: AvcCBox): ByteBuffer {
        return saveCodecPrivate(avcC.getSpsList(), avcC.getPpsList())
    }

    @JvmStatic
    fun parseAVCCFromBuffer(bb: ByteBuffer?): AvcCBox {
        return parseAvcCBox(bb!!)
    }

    fun writeSPS(sps: SeqParameterSet, approxSize: Int): ByteBuffer {
        val output = ByteBuffer.allocate(approxSize + 8)
        sps.write(output)
        output.flip()
        escapeNALinplace(output)
        return output
    }

    @JvmStatic
    fun readSPS(data: ByteBuffer?): SeqParameterSet {
        val input = NIOUtils.duplicate(data)
        unescapeNAL(input)
        return SeqParameterSet.read(input)
    }

    fun writePPS(pps: PictureParameterSet, approxSize: Int): ByteBuffer {
        val output = ByteBuffer.allocate(approxSize + 8)
        pps.write(output)
        output.flip()
        escapeNALinplace(output)
        return output
    }

    fun readPPS(data: ByteBuffer?): PictureParameterSet {
        val input = NIOUtils.duplicate(data)
        unescapeNAL(input)
        return PictureParameterSet.read(input)
    }

    fun findPPS(ppss: List<PictureParameterSet>?, id: Int): PictureParameterSet? {
        for (pps in ppss!!) {
            if (pps.picParameterSetId == id) return pps
        }
        return null
    }

    fun findSPS(spss: List<SeqParameterSet>?, id: Int): SeqParameterSet? {
        for (sps in spss!!) {
            if (sps.seqParameterSetId == id) return sps
        }
        return null
    }

    fun getPicSize(sps: SeqParameterSet): Size {
        var w = sps.picWidthInMbsMinus1 + 1 shl 4
        var h = getPicHeightInMbs(sps) shl 4
        if (sps.isFrameCroppingFlag) {
            w -= sps.frameCropLeftOffset + sps.frameCropRightOffset shl sps.chromaFormatIdc!!.compWidth[1]
            h -= sps.frameCropTopOffset + sps.frameCropBottomOffset shl sps.chromaFormatIdc!!.compHeight[1]
        }
        return Size(w, h)
    }

    @JvmStatic
    fun readSPSFromBufferList(spsList: List<ByteBuffer?>): List<SeqParameterSet> {
        val result: MutableList<SeqParameterSet> = ArrayList()
        for (byteBuffer in spsList) {
            result.add(readSPS(NIOUtils.duplicate(byteBuffer)))
        }
        return result
    }

    @JvmStatic
    fun readPPSFromBufferList(ppsList: List<ByteBuffer?>): List<PictureParameterSet> {
        val result: MutableList<PictureParameterSet> = ArrayList()
        for (byteBuffer in ppsList) {
            result.add(readPPS(NIOUtils.duplicate(byteBuffer)))
        }
        return result
    }

    fun writePPSList(allPps: List<PictureParameterSet>): List<ByteBuffer> {
        val result: MutableList<ByteBuffer> = ArrayList()
        for (pps in allPps) {
            result.add(writePPS(pps, 64))
        }
        return result
    }

    fun writeSPSList(allSps: List<SeqParameterSet>): List<ByteBuffer> {
        val result: MutableList<ByteBuffer> = ArrayList()
        for (sps in allSps) {
            result.add(writeSPS(sps, 256))
        }
        return result
    }

    @Throws(IOException::class)
    fun dumpFrame(ch: FileChannelWrapper, values: Array<SeqParameterSet>, values2: Array<PictureParameterSet>,
                  nalUnits: List<ByteBuffer>) {
        for (i in values.indices) {
            val sps = values[i]
            NIOUtils.writeInt(ch, 1)
            NIOUtils.writeByte(ch, 0x67.toByte())
            ch.write(writeSPS(sps, 128))
        }
        for (i in values2.indices) {
            val pps = values2[i]
            NIOUtils.writeInt(ch, 1)
            NIOUtils.writeByte(ch, 0x68.toByte())
            ch.write(writePPS(pps, 256))
        }
        for (byteBuffer in nalUnits) {
            NIOUtils.writeInt(ch, 1)
            ch.write(byteBuffer.duplicate())
        }
    }

    fun toNAL(codecPrivate: ByteBuffer, sps: SeqParameterSet, pps: PictureParameterSet) {
        val bb1 = ByteBuffer.allocate(512)
        val bb2 = ByteBuffer.allocate(512)
        sps.write(bb1)
        pps.write(bb2)
        bb1.flip()
        bb2.flip()
        putNAL(codecPrivate, bb1, 0x67)
        putNAL(codecPrivate, bb2, 0x68)
    }

    fun toNALList(codecPrivate: ByteBuffer, spsList2: List<ByteBuffer>, ppsList2: List<ByteBuffer>) {
        for (byteBuffer in spsList2) putNAL(codecPrivate, byteBuffer, 0x67)
        for (byteBuffer in ppsList2) putNAL(codecPrivate, byteBuffer, 0x68)
    }

    private fun putNAL(codecPrivate: ByteBuffer, byteBuffer: ByteBuffer, nalType: Int) {
        val dst = ByteBuffer.allocate(byteBuffer.remaining() * 2)
        escapeNAL(byteBuffer, dst)
        dst.flip()
        codecPrivate.putInt(1)
        codecPrivate.put(nalType.toByte())
        codecPrivate.put(dst)
    }

    /**
     * Parses a list of SPS NAL units out of the codec private array.
     *
     * @param codecPrivate
     * An AnnexB formatted set of SPS/PPS NAL units.
     * @return A list of ByteBuffers containing PPS NAL units.
     */
    fun getRawPPS(codecPrivate: ByteBuffer): List<ByteBuffer> {
        return getRawNALUnitsOfType(codecPrivate, NALUnitType.PPS)
    }

    /**
     * Parses a list of SPS NAL units out of the codec private array.
     *
     * @param codecPrivate
     * An AnnexB formatted set of SPS/PPS NAL units.
     * @return A list of ByteBuffers containing SPS NAL units.
     */
    fun getRawSPS(codecPrivate: ByteBuffer): List<ByteBuffer> {
        return getRawNALUnitsOfType(codecPrivate, NALUnitType.SPS)
    }

    fun getRawNALUnitsOfType(codecPrivate: ByteBuffer, type: NALUnitType): List<ByteBuffer> {
        val result: MutableList<ByteBuffer> = ArrayList()
        for (bb in splitFrame(codecPrivate.duplicate())) {
            val nu = read(bb)
            if (nu.type == type) {
                result.add(bb)
            }
        }
        return result
    }

    abstract class SliceHeaderTweaker {
        protected var sps: List<SeqParameterSet>? = null
        protected var pps: List<PictureParameterSet>? = null
        protected abstract fun tweak(sh: SliceHeader?)
        fun run(`is`: ByteBuffer, os: ByteBuffer, nu: NALUnit): SliceHeader {
            val nal = os.duplicate()
            unescapeNAL(`is`)
            val reader = BitReader.createBitReader(`is`)
            val sh = readPart1(reader)
            val pp = findPPS(pps, sh.picParameterSetId)
            return part2(`is`, os, nu, findSPS(sps, pp!!.picParameterSetId), pp, nal, reader, sh)
        }

        fun runSpsPps(`is`: ByteBuffer, os: ByteBuffer, nu: NALUnit, sps: SeqParameterSet?,
                      pps: PictureParameterSet?): SliceHeader {
            val nal = os.duplicate()
            unescapeNAL(`is`)
            val reader = BitReader.createBitReader(`is`)
            val sh = readPart1(reader)
            return part2(`is`, os, nu, sps, pps, nal, reader, sh)
        }

        private fun part2(`is`: ByteBuffer, os: ByteBuffer, nu: NALUnit, sps: SeqParameterSet?,
                          pps: PictureParameterSet?, nal: ByteBuffer, reader: BitReader, sh: SliceHeader): SliceHeader {
            val writer = BitWriter(os)
            readPart2(sh, nu, sps!!, pps!!, reader)
            tweak(sh)
            write(sh, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, writer)
            if (pps.isEntropyCodingModeFlag) copyDataCABAC(`is`, os, reader, writer) else copyDataCAVLC(`is`, os, reader, writer)
            nal.limit(os.position())
            escapeNALinplace(nal)
            os.position(nal.limit())
            return sh
        }

        private fun copyDataCAVLC(`is`: ByteBuffer, os: ByteBuffer, reader: BitReader, writer: BitWriter) {
            val wLeft = 8 - writer.curBit()
            if (wLeft != 0) writer.writeNBit(reader.readNBit(wLeft), wLeft)
            writer.flush()

            // Copy with shift
            val shift = reader.curBit()
            if (shift != 0) {
                val mShift = 8 - shift
                var inp = reader.readNBit(mShift)
                reader.stop()
                while (`is`.hasRemaining()) {
                    var out = inp shl shift
                    inp = `is`.get() and 0xff
                    out = out or (inp shr mShift)
                    os.put(out.toByte())
                }
                os.put((inp shl shift).toByte())
            } else {
                reader.stop()
                os.put(`is`)
            }
        }

        private fun copyDataCABAC(`is`: ByteBuffer, os: ByteBuffer, reader: BitReader, writer: BitWriter) {
            val bp = reader.curBit().toLong()
            if (bp != 0L) {
                val rem = reader.readNBit(8 - bp.toInt()).toLong()
                if ((1 shl (8 - bp).toInt()) - 1.toLong() != rem) throw RuntimeException("Invalid CABAC padding")
            }
            if (writer.curBit() != 0) writer.writeNBit(0xff, 8 - writer.curBit())
            writer.flush()
            reader.stop()
            os.put(`is`)
        }
    }

    /**
     * A collection of functions to work with a compact representation of a motion vector.
     *
     * Motion vector is represented as long:
     *
     * ||rrrrrr|vvvvvvvvvvvv|hhhhhhhhhhhhhh||
     *
     */
    object Mv {
        @JvmStatic
        fun mvX(mv: Int): Int {
            return mv shl 18 shr 18
        }

        @JvmStatic
        fun mvY(mv: Int): Int {
            return mv shl 6 shr 20
        }

        @JvmStatic
        fun mvRef(mv: Int): Int {
            return mv shr 26
        }

        @JvmStatic
        fun packMv(mvx: Int, mvy: Int, r: Int): Int {
            return r and 0x3f shl 26 or (mvy and 0xfff shl 14) or (mvx and 0x3fff)
        }

        @JvmStatic
        fun mvC(mv: Int, comp: Int): Int {
            return if (comp == 0) mvX(mv) else mvY(mv)
        }
    }

    /**
     * A collection of functions to work with a compact representation of a
     * motion vector list.
     *
     * Motion vector list contains interleaved pairs of forward and backward
     * motion vectors packed into integers.
     *
     */
    class MvList(size: Int) {
        private val list: IntArray
        fun clear() {
            var i = 0
            while (i < list.size) {
                list[i + 1] = NA
                list[i] = list[i + 1]
                i += 2
            }
        }

        fun mv0X(off: Int): Int {
            return Mv.mvX(list[off shl 1])
        }

        fun mv0Y(off: Int): Int {
            return Mv.mvY(list[off shl 1])
        }

        fun mv0R(off: Int): Int {
            return Mv.mvRef(list[off shl 1])
        }

        fun mv1X(off: Int): Int {
            return Mv.mvX(list[(off shl 1) + 1])
        }

        fun mv1Y(off: Int): Int {
            return Mv.mvY(list[(off shl 1) + 1])
        }

        fun mv1R(off: Int): Int {
            return Mv.mvRef(list[(off shl 1) + 1])
        }

        fun getMv(off: Int, forward: Int): Int {
            return list[(off shl 1) + forward]
        }

        fun setMv(off: Int, forward: Int, mv: Int) {
            list[(off shl 1) + forward] = mv
        }

        fun setPair(off: Int, mv0: Int, mv1: Int) {
            list[off shl 1] = mv0
            list[(off shl 1) + 1] = mv1
        }

        fun copyPair(off: Int, other: MvList, otherOff: Int) {
            list[off shl 1] = other.list[otherOff shl 1]
            list[(off shl 1) + 1] = other.list[(otherOff shl 1) + 1]
        }

        companion object {
            private val NA = Mv.packMv(0, 0, -1)
        }

        init {
            list = IntArray(size shl 1)
            clear()
        }
    }

    class MvList2D(width: Int, height: Int) {
        private val list: IntArray
        private val stride: Int
        @JvmField
        val width: Int
        @JvmField
        val height: Int
        fun clear() {
            var i = 0
            while (i < list.size) {
                list[i + 1] = NA
                list[i] = list[i + 1]
                i += 2
            }
        }

        fun mv0X(offX: Int, offY: Int): Int {
            return Mv.mvX(list[(offX shl 1) + stride * offY])
        }

        fun mv0Y(offX: Int, offY: Int): Int {
            return Mv.mvY(list[(offX shl 1) + stride * offY])
        }

        fun mv0R(offX: Int, offY: Int): Int {
            return Mv.mvRef(list[(offX shl 1) + stride * offY])
        }

        fun mv1X(offX: Int, offY: Int): Int {
            return Mv.mvX(list[(offX shl 1) + stride * offY + 1])
        }

        fun mv1Y(offX: Int, offY: Int): Int {
            return Mv.mvY(list[(offX shl 1) + stride * offY + 1])
        }

        fun mv1R(offX: Int, offY: Int): Int {
            return Mv.mvRef(list[(offX shl 1) + stride * offY + 1])
        }

        fun getMv(offX: Int, offY: Int, forward: Int): Int {
            return list[(offX shl 1) + stride * offY + forward]
        }

        fun setMv(offX: Int, offY: Int, forward: Int, mv: Int) {
            list[(offX shl 1) + stride * offY + forward] = mv
        }

        companion object {
            private val NA = Mv.packMv(0, 0, -1)
        }

        init {
            list = IntArray((width shl 1) * height)
            stride = width shl 1
            this.width = width
            this.height = height
            clear()
        }
    }
}