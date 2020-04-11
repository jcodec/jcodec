package org.jcodec.codecs.mjpeg

import org.jcodec.asByteBuffer
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.scale.AWTUtil
import org.jcodec.scale.Yuv420jToRgb
import org.junit.Test
import java.io.File

class JpegDecoderTest {
    @Test
    fun testDecode() {
        val decoder = JpegDecoder()
        val picture = Picture.create(272, 200, ColorSpace.YUV420J)
        val data = picture.data
        val asByteBuffer = File("src/test/resources/jpegdecoderselfie.jpg").readBytes().asByteBuffer()
        decoder.decodeFrame(asByteBuffer, data)
        val rgb = Picture.create(272, 200, ColorSpace.RGB)
        Yuv420jToRgb().transform(picture, rgb)
        AWTUtil.writePNG(rgb, File("tmp/dec.png"))
    }
}