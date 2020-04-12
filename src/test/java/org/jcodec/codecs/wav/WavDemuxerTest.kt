package org.jcodec.codecs.wav

import org.jcodec.asSequence
import org.jcodec.common.AudioFormat
import org.jcodec.common.io.NIOUtils
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class WavDemuxerTest {

    @Test
    fun wavDemuxer() {
        val actual = WavDemuxer(NIOUtils.readableChannel(File("src/test/resources/mono_sine_decoded.wav"))).use {
            val meta = it.meta
            val format = meta.audioCodecMeta!!.format
            assertEquals(AudioFormat.MONO_48K_S16_LE, format)
            it.audioTracks.first().asSequence().toList().sumBy { it.data.remaining() }
        }
        assertEquals(90 * 1024, actual)
    }
}