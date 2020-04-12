package org.jcodec.common

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
data class AudioFormat(val sampleRate: Int, val sampleSizeInBits: Int, val channels: Int, val isSigned: Boolean, val isBigEndian: Boolean) {

    val frameSize: Short
        get() = ((sampleSizeInBits shr 3) * channels).toShort()

    val frameRate: Int
        get() = sampleRate

    fun bytesToFrames(bytes: Int): Int {
        return bytes / (channels * sampleSizeInBits shr 3)
    }

    fun framesToBytes(samples: Int): Int {
        return samples * (channels * sampleSizeInBits shr 3)
    }

    fun bytesToSamples(bytes: Int): Int {
        return bytes / (sampleSizeInBits shr 3)
    }

    fun samplesToBytes(samples: Int): Int {
        return samples * (sampleSizeInBits shr 3)
    }

    companion object {
        // Common audio formats
        var STEREO_48K_S16_BE = AudioFormat(48000, 16, 2, true, true)
        var STEREO_48K_S16_LE = AudioFormat(48000, 16, 2, true, false)
        var STEREO_48K_S24_BE = AudioFormat(48000, 24, 2, true, true)
        var STEREO_48K_S24_LE = AudioFormat(48000, 24, 2, true, false)
        var MONO_48K_S16_BE = AudioFormat(48000, 16, 1, true, true)
        var MONO_48K_S16_LE = AudioFormat(48000, 16, 1, true, false)
        var MONO_48K_S24_BE = AudioFormat(48000, 24, 1, true, true)
        var MONO_48K_S24_LE = AudioFormat(48000, 24, 1, true, false)
        var STEREO_44K_S16_BE = AudioFormat(44100, 16, 2, true, true)
        var STEREO_44K_S16_LE = AudioFormat(44100, 16, 2, true, false)
        var STEREO_44K_S24_BE = AudioFormat(44100, 24, 2, true, true)
        var STEREO_44K_S24_LE = AudioFormat(44100, 24, 2, true, false)
        var MONO_44K_S16_BE = AudioFormat(44100, 16, 1, true, true)
        var MONO_44K_S16_LE = AudioFormat(44100, 16, 1, true, false)
        var MONO_44K_S24_BE = AudioFormat(44100, 24, 1, true, true)
        var MONO_44K_S24_LE = AudioFormat(44100, 24, 1, true, false)
        fun STEREO_S16_BE(rate: Int): AudioFormat {
            return AudioFormat(rate, 16, 2, true, true)
        }

        fun STEREO_S16_LE(rate: Int): AudioFormat {
            return AudioFormat(rate, 16, 2, true, false)
        }

        fun STEREO_S24_BE(rate: Int): AudioFormat {
            return AudioFormat(rate, 24, 2, true, true)
        }

        fun STEREO_S24_LE(rate: Int): AudioFormat {
            return AudioFormat(rate, 24, 2, true, false)
        }

        fun MONO_S16_BE(rate: Int): AudioFormat {
            return AudioFormat(rate, 16, 1, true, true)
        }

        fun MONO_S16_LE(rate: Int): AudioFormat {
            return AudioFormat(rate, 16, 1, true, false)
        }

        fun MONO_S24_BE(rate: Int): AudioFormat {
            return AudioFormat(rate, 24, 1, true, true)
        }

        fun MONO_S24_LE(rate: Int): AudioFormat {
            return AudioFormat(rate, 24, 1, true, false)
        }

        fun NCH_48K_S16_BE(n: Int): AudioFormat {
            return AudioFormat(48000, 16, n, true, true)
        }

        fun NCH_48K_S16_LE(n: Int): AudioFormat {
            return AudioFormat(48000, 16, n, true, false)
        }

        fun NCH_48K_S24_BE(n: Int): AudioFormat {
            return AudioFormat(48000, 24, n, true, true)
        }

        fun NCH_48K_S24_LE(n: Int): AudioFormat {
            return AudioFormat(48000, 24, n, true, false)
        }

        fun NCH_44K_S16_BE(n: Int): AudioFormat {
            return AudioFormat(44100, 16, n, true, true)
        }

        fun NCH_44K_S16_LE(n: Int): AudioFormat {
            return AudioFormat(44100, 16, n, true, false)
        }

        fun NCH_44K_S24_BE(n: Int): AudioFormat {
            return AudioFormat(44100, 24, n, true, true)
        }

        fun NCH_44K_S24_LE(n: Int): AudioFormat {
            return AudioFormat(44100, 24, n, true, false)
        }

        fun createAudioFormat(format: AudioFormat): AudioFormat {
            return AudioFormat(format.frameRate, format.sampleSizeInBits, format.channels, format.isSigned, format.isBigEndian)
        }

        fun createAudioFormat2(format: AudioFormat, newSampleRate: Int): AudioFormat {
            return format.copy(sampleRate = newSampleRate)
        }
    }

}