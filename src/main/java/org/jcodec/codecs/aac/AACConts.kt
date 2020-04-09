package org.jcodec.codecs.aac

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object AACConts {
    val AAC_CHANNEL_COUNT = shortArrayOf(0, 1, 2, 3, 4, 5, 6, 8)
    @JvmField
    val AAC_SAMPLE_RATES = intArrayOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000,
            11025, 8000, 7350)
}