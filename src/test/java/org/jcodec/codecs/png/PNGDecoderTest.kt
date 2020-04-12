package org.jcodec.codecs.png

import org.jcodec.asByteBuffer
import org.jcodec.codecs.ppm.PPMEncoder
import org.jcodec.codecs.util.PGMIO
import org.jcodec.common.Preconditions
import org.jcodec.common.and
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.md5
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PNGDecoderTest {

    @Test
    //this test fails because we dont support 4bit per pixel gray images
    fun testTransparentGray4() {
        val pngDec = PNGDecoder()
        val buf = File("src/test/resources/png/tbbn0g04.png").readBytes().asByteBuffer()
        val meta = pngDec.getCodecMeta(buf)!!
        val pic = Picture.create(meta.size.width, meta.size.height, ColorSpace.RGB)
        val dec = pngDec.decodeFrame(buf, pic.data)!!
        val encodeFrame = PPMEncoder().encodeFrame(dec)
        NIOUtils.writeTo(encodeFrame, File("tmp/tbbn0g04.ppm"))
        assertEquals("0f6ac4f2f13a4c94e98552549a80b6b2", dec.data[0].md5())
    }

    @Test
    fun testTransparentPalette8() {
        val pngDec = PNGDecoder()
        val buf = File("src/test/resources/png/tp1n3p08.png").readBytes().asByteBuffer()
        val meta = pngDec.getCodecMeta(buf)!!
        val pic = Picture.create(meta.size.width, meta.size.height, ColorSpace.RGB)
        val dec = pngDec.decodeFrame(buf, pic.data)!!
        assertEquals("b4e325ced1e8a1a932fefa1716c20799", dec.data[0].md5())
    }

    @Test
    fun testTransparentTrueColor() {
        val pngDec = PNGDecoder()
        val buf = File("src/test/resources/png/tbrn2c08.png").readBytes().asByteBuffer()
        val meta = pngDec.getCodecMeta(buf)!!
        val pic = Picture.create(meta.size.width, meta.size.height, ColorSpace.RGB)
        val dec = pngDec.decodeFrame(buf, pic.data)!!
        assertEquals("9d0e144e9612f08614935942289ae33c", dec.data[0].md5())
    }

    @Test
    fun testProbe() {
        assertTrue(50 <= PNGDecoder.probe(File("src/test/resources/png/img0.png").readBytes().asByteBuffer()))
        assertTrue(50 >= PNGDecoder.probe(File("src/test/resources/png/img0.raw").readBytes().asByteBuffer()))
    }

    @Test
    fun testPNG() {
        val dir = "src/test/resources/png/img%d.png"
        val raw = "src/test/resources/png/img%d.raw"
        val pngDec = PNGDecoder()
        for (i in 0..6) {
            val f = File(String.format(dir, i))
            Preconditions.checkState(f.exists(), "no such file " + f.path)
            val buf = f.readBytes().asByteBuffer()
            val codecMeta = pngDec.getCodecMeta(buf)!!
            val pic = Picture.create(codecMeta.size.width, codecMeta.size.height,
                    ColorSpace.RGB)
            val dec = pngDec.decodeFrame(buf, pic.data)!!
            val refB = NIOUtils.fetchFromFile(File(String.format(raw, i)))
            val array = NIOUtils.toArray(refB)
            for (j in array.indices) {
                array[j] = ((array[j] and 0xff) - 128).toByte()
            }
            Assert.assertArrayEquals("" + i, array, dec.getPlaneData(0))
        }
    }
}