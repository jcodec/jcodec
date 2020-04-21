package org.jcodec.codecs.raw

import org.jcodec.common.model.ColorSpace.Companion.YUV422
import org.jcodec.parseHex
import org.jcodec.toHex
import org.junit.Test
import kotlin.test.assertEquals

class V210DecoderTest {
    @Test
    fun decode() {
        val decode = V210Decoder(48, 16).decode("01030930".repeat(4 * 128).parseHex())
        assertEquals(YUV422, decode.color)
        val data = decode.data
        assertEquals(48 * 16, data[0].size)
        assertEquals(48 * 16 / 2, data[1].size)
        assertEquals(48 * 16 / 2, data[2].size)
        val yhex = data[0].toHex()
        val uhex = data[1].toHex()
        val vhex = data[2].toHex()
        val yexpected = "104040".repeat(48 * 16 / 3)
        val uexpected = "401040".repeat(48 * 16 / 6)
        val vexpected = "404010".repeat(48 * 16 / 6)
        assertEquals(yexpected, yhex)
        assertEquals(uexpected, uhex)
        assertEquals(vexpected, vhex)
    }
}