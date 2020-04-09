package org.jcodec.api

import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.model.Picture

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class PictureWithMetadata(val picture: Picture, val timestamp: Double, val duration: Double, val orientation: DemuxerTrackMeta.Orientation) {

    companion object {
        @JvmStatic
        fun createPictureWithMetadata(picture: Picture, timestamp: Double, duration: Double): PictureWithMetadata {
            return PictureWithMetadata(picture, timestamp, duration, DemuxerTrackMeta.Orientation.D_0)
        }
    }

}