package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Track fragment box
 *
 * Contains routines dedicated to simplify working with track fragments
 *
 * @author The JCodec project
 */
class TrackFragmentBox(atom: Header) : NodeBox(atom) {
    val trackId: Int
        get() {
            val tfhd = (findFirst(this, TrackFragmentHeaderBox.fourcc()) as TrackFragmentHeaderBox?)
                    ?: throw RuntimeException("Corrupt track fragment, no header atom found")
            return tfhd.getTrackId()
        }

    val trun: TrunBox
        get() = findFirst(this, TrunBox.fourcc()) as TrunBox

    val tfhd: TrackFragmentHeaderBox
        get() = findFirst(this, TrackFragmentHeaderBox.fourcc()) as TrackFragmentHeaderBox

    val tfdt: TrackFragmentBaseMediaDecodeTimeBox
        get() = findFirst(this, TrackFragmentBaseMediaDecodeTimeBox.fourcc()) as TrackFragmentBaseMediaDecodeTimeBox

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "traf"
        }

        fun createTrackFragmentBox(): TrackFragmentBox {
            return TrackFragmentBox(Header(fourcc()))
        }
    }
}