package org.jcodec.codecs.wav

import org.jcodec.asSequence
import org.jcodec.common.io.NIOUtils
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class WavDemuxerTest {

    @Test
    fun wavDemuxer() {
        val actual = WavDemuxer(NIOUtils.readableChannel(File("src/test/resources/mono_sine_decoded.wav"))).use {
            it.audioTracks.first().asSequence().toList().sumBy { it.data.remaining() }
        }
        assertEquals(90 * 1024, actual)
    }
}