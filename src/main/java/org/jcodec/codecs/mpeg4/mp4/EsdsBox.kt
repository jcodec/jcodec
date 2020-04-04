package org.jcodec.codecs.mpeg4.mp4

import org.jcodec.codecs.aac.ADTSParser
import org.jcodec.codecs.mpeg4.es.*
import org.jcodec.containers.mp4.boxes.FullBox
import org.jcodec.containers.mp4.boxes.Header
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * MPEG 4 elementary stream descriptor
 *
 * @author The JCodec project
 */
class EsdsBox(atom: Header) : FullBox(atom) {
    var streamInfo: ByteBuffer? = null
        private set
    var objectType = 0
        private set
    var bufSize = 0
        private set
    var maxBitrate = 0
        private set
    var avgBitrate = 0
        private set
    var trackId = 0
        private set

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        if (streamInfo != null && streamInfo!!.remaining() > 0) {
            val l = ArrayList<Descriptor>()
            val l1 = ArrayList<Descriptor>()
            l1.add(DecoderSpecific(streamInfo))
            l.add(DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, l1))
            l.add(SL())
            ES(trackId, l).write(out)
        } else {
            val l = ArrayList<Descriptor>()
            l.add(DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, ArrayList()))
            l.add(SL())
            ES(trackId, l).write(out)
        }
    }

    override fun estimateSize(): Int {
        return 64
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val es = DescriptorParser.read(input) as ES
        trackId = es.trackId
        val decoderConfig = NodeDescriptor.findByTag<DecoderConfig>(es, DecoderConfig.tag())
        objectType = decoderConfig.objectType
        bufSize = decoderConfig.bufSize
        maxBitrate = decoderConfig.maxBitrate
        avgBitrate = decoderConfig.avgBitrate
        val decoderSpecific = NodeDescriptor.findByTag<DecoderSpecific>(decoderConfig, DecoderSpecific.tag())
        streamInfo = decoderSpecific?.data
    }

    companion object {
        fun fourcc(): String {
            return "esds"
        }

        @JvmStatic
        fun fromADTS(hdr: ADTSParser.Header): EsdsBox {
            return createEsdsBox(ADTSParser.adtsToStreamInfo(hdr), hdr.objectType shl 5, 0, 210750, 133350, 2)
        }

        fun createEsdsBox(streamInfo: ByteBuffer?, objectType: Int, bufSize: Int, maxBitrate: Int,
                          avgBitrate: Int, trackId: Int): EsdsBox {
            val esds = EsdsBox(Header(fourcc()))
            esds.objectType = objectType
            esds.bufSize = bufSize
            esds.maxBitrate = maxBitrate
            esds.avgBitrate = avgBitrate
            esds.trackId = trackId
            esds.streamInfo = streamInfo
            return esds
        }

        fun newEsdsBox(): EsdsBox {
            return EsdsBox(Header(fourcc()))
        }
    }
}