package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Movie fragment box and dedicated routines
 *
 * @author The JCodec project
 */
class MovieFragmentBox(atom: Header) : NodeBox(atom) {
    var movie: MovieBox? = null

    val tracks: Array<TrackFragmentBox>
        get() = findAll(this, TrackFragmentBox.fourcc()).map { it as TrackFragmentBox }.toTypedArray()

    val sequenceNumber: Int
        get() {
            val mfhd = (findFirst(this, MovieFragmentHeaderBox.fourcc()) as MovieFragmentBox?)
                    ?: throw RuntimeException("Corrupt movie fragment, no header atom found")
            return mfhd.sequenceNumber
        }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "moof"
        }

        fun createMovieFragmentBox(): MovieFragmentBox {
            return MovieFragmentBox(Header(fourcc()))
        }
    }
}