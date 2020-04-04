package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Movie fragment header box
 *
 *
 * @author The JCodec project
 */
class TrackExtendsBox(atom: Header) : FullBox(atom) {
    var trackId = 0
    var defaultSampleDescriptionIndex = 0
    var defaultSampleDuration = 0
    var defaultSampleBytes = 0
    var defaultSampleFlags = 0
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        trackId = input.int
        defaultSampleDescriptionIndex = input.int
        defaultSampleDuration = input.int
        defaultSampleBytes = input.int
        defaultSampleFlags = input.int
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(trackId)
        out.putInt(defaultSampleDescriptionIndex)
        out.putInt(defaultSampleDuration)
        out.putInt(defaultSampleBytes)
        out.putInt(defaultSampleFlags)
    }

    override fun estimateSize(): Int {
        return 32
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "trex"
        }

        fun createTrackExtendsBox(): TrackExtendsBox {
            return TrackExtendsBox(Header(fourcc()))
        }
    }
}