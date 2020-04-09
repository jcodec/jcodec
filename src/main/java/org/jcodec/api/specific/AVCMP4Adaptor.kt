package org.jcodec.api.specific

import org.jcodec.api.MediaInfo
import org.jcodec.codecs.h264.H264Decoder
import org.jcodec.codecs.h264.H264Decoder.Companion.createH264DecoderFromCodecPrivate
import org.jcodec.codecs.h264.H264Utils.idrSlice
import org.jcodec.codecs.h264.H264Utils.nextNALUnit
import org.jcodec.codecs.h264.H264Utils.readSPS
import org.jcodec.codecs.h264.H264Utils.splitFrame
import org.jcodec.codecs.h264.io.model.NALUnit.Companion.read
import org.jcodec.codecs.h264.io.model.NALUnitType
import org.jcodec.codecs.h264.io.model.SeqParameterSet.Companion.getPicHeightInMbs
import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Size
import org.jcodec.containers.mp4.MP4Packet
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * High level frame grabber helper.
 *
 * @author The JCodec project
 */
class AVCMP4Adaptor(private val meta: DemuxerTrackMeta) : ContainerAdaptor {
    private var decoder: H264Decoder? = null
    private var curENo: Int
    private var size: Size? = null
    private fun calcBufferSize() {
        var w = Int.MIN_VALUE
        var h = Int.MIN_VALUE
        val bb = meta.codecPrivate!!.duplicate()
        var b: ByteBuffer?
        while (nextNALUnit(bb).also { b = it } != null) {
            val nu = read(b!!)
            if (nu.type != NALUnitType.SPS) continue
            val sps = readSPS(b)
            val ww = sps.picWidthInMbsMinus1 + 1
            if (ww > w) w = ww
            val hh = getPicHeightInMbs(sps)
            if (hh > h) h = hh
        }
        size = Size(w shl 4, h shl 4)
    }

    override fun decodeFrame(packet: Packet, data: Array<ByteArray>): Picture {
        updateState(packet)
        val pic: Picture = decoder!!.decodeFrame(packet.getData(), data)
        val pasp = meta.videoCodecMeta!!.pixelAspectRatio
        if (pasp != null) {
            // TODO: transform
        }
        return pic
    }

    private fun updateState(packet: Packet) {
        val eNo = (packet as MP4Packet).entryNo
        if (eNo != curENo) {
            curENo = eNo
            //            avcCBox = H264Utils.parseAVCC((VideoSampleEntry) ses[curENo]);
//            decoder = new H264Decoder();
//            ((H264Decoder) decoder).addSps(avcCBox.getSpsList());
//            ((H264Decoder) decoder).addPps(avcCBox.getPpsList());
        }
        if (decoder == null) {
            decoder = createH264DecoderFromCodecPrivate(meta.codecPrivate!!)
        }
    }

    override fun canSeek(pkt: Packet): Boolean {
        updateState(pkt)
        return idrSlice(splitFrame(pkt.getData()))
    }

    override fun allocatePicture(): Array<ByteArray> {
        return Picture.create(size!!.width, size!!.height, ColorSpace.YUV444).data
    }

    override fun getMediaInfo(): MediaInfo {
        return MediaInfo(size)
    }

    init {
        curENo = -1
        calcBufferSize()
    }
}