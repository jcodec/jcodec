package org.jcodec.common

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Meta information about this media track.
 *
 * @author The JCodec project
 */
open class DemuxerTrackMeta(val type: TrackType?, val codec: Codec?,
                            /**
                             * @return Total duration in seconds of the media track
                             */
                            val totalDuration: Double,
                            /**
                             * @return Array of frame indexes that can be used to seek to, i.e. which
                             * don't require any previous frames to be decoded. Is null when
                             * every frame is a seek frame.
                             */
                            val seekFrames: IntArray?,
                            /**
                             * @return Total number of frames in this media track.
                             */
                            val totalFrames: Int, val codecPrivate: ByteBuffer?, val videoCodecMeta: VideoCodecMeta?, val audioCodecMeta: AudioCodecMeta?) {

    var index = 0
    @JvmField
    var orientation: Orientation

    enum class Orientation {
        D_0, D_90, D_180, D_270
    }

    init {
        orientation = Orientation.D_0
    }
}