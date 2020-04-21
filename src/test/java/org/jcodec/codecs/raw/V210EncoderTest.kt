package org.jcodec.codecs.raw

import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.toByteArray
import org.jcodec.toHex
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class V210EncoderTest {
    @Test
    fun encode() {
        val encodeFrame = V210Encoder().encodeFrame(ByteBuffer.allocate(48 * 16 * 3), Picture.create(48, 16, ColorSpace.YUV422))
        val expected = "00020820".repeat(4 * 128)
        val actual = encodeFrame.toByteArray().toHex()
        assertEquals(expected, actual);
    }
}