package org.jcodec.containers.mp4

import org.jcodec.common.TrackType

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
class MP4TrackType private constructor(val handler: String, val trackType: TrackType) {

    companion object {
        @JvmField
        val VIDEO = MP4TrackType("vide", TrackType.VIDEO)
        @JvmField
        val SOUND = MP4TrackType("soun", TrackType.AUDIO)
        val TIMECODE = MP4TrackType("tmcd", TrackType.OTHER)
        val HINT = MP4TrackType("hint", TrackType.OTHER)
        val TEXT = MP4TrackType("text", TrackType.OTHER)
        val HYPER_TEXT = MP4TrackType("wtxt", TrackType.OTHER)
        val CC = MP4TrackType("clcp", TrackType.OTHER)
        val SUB = MP4TrackType("sbtl", TrackType.OTHER)
        val MUSIC = MP4TrackType("musi", TrackType.AUDIO)
        val MPEG1 = MP4TrackType("MPEG", TrackType.VIDEO)
        val SPRITE = MP4TrackType("sprt", TrackType.OTHER)
        val TWEEN = MP4TrackType("twen", TrackType.OTHER)
        val CHAPTERS = MP4TrackType("chap", TrackType.OTHER)
        val THREE_D = MP4TrackType("qd3d", TrackType.OTHER)
        val STREAMING = MP4TrackType("strm", TrackType.OTHER)
        val OBJECTS = MP4TrackType("obje", TrackType.OTHER)
        val DATA = MP4TrackType("url ", TrackType.OTHER)
        val META = MP4TrackType("meta", TrackType.META)
        private val _values = arrayOf(VIDEO, SOUND, TIMECODE, HINT, TEXT, HYPER_TEXT, CC,
                SUB, MUSIC, MPEG1, SPRITE, TWEEN, CHAPTERS, THREE_D, STREAMING, OBJECTS, DATA, META)

        fun fromHandler(handler: String): MP4TrackType? {
            for (i in _values.indices) {
                val `val` = _values[i]
                if (`val`.handler == handler) return `val`
            }
            return null
        }
    }

}