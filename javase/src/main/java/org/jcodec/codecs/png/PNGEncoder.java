package org.jcodec.codecs.png;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToBgr8Bit;

/**
 * Video encoder interface wrapper to Java SE png functionality.
 * 
 * @author Stanislav Vitvitskiy
 */
public class PNGEncoder extends VideoEncoder {
    private BufferedImage bi;
    private RgbToBgr8Bit rgbToBgr;

    @Override
    public EncodedFrame encodeFrame8Bit(Picture8Bit pic, ByteBuffer _out) {
        if (bi == null) {
            bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        }
        if (rgbToBgr == null) {
            rgbToBgr = new RgbToBgr8Bit();
        }
        rgbToBgr.transform(pic, pic);

        AWTUtil.toBufferedImage8Bit(pic, bi);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bi, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new EncodedFrame(ByteBuffer.wrap(baos.toByteArray()), true);
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.RGB };
    }

    @Override
    public int estimateBufferSize(Picture8Bit frame) {
        // We assume it's the same size as raw image
        return frame.getWidth() * frame.getHeight() * 3;
    }

}
