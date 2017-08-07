package org.jcodec.codecs.pngawt;

import static org.jcodec.common.model.ColorSpace.BGR;
import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToBgr;

/**
 * Video encoder interface wrapper to Java SE png functionality.
 * 
 * @author Stanislav Vitvitskiy
 */
public class PNGEncoder extends VideoEncoder {
    private BufferedImage bi;
    private RgbToBgr rgbToBgr;

    @Override
    public EncodedFrame encodeFrame(Picture _pic, ByteBuffer _out) {
        Picture pic = _pic.cropped();
        if (bi == null) {
            bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        }
        if (rgbToBgr == null) {
            rgbToBgr = new RgbToBgr();
        }
        if (pic.getColor() == RGB)
            rgbToBgr.transform(pic, pic);
        else if (pic.getColor() != BGR)
            throw new IllegalArgumentException("Unsupported input color space: " + pic.getColor());

        AWTUtil.toBufferedImage(pic, bi);
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
    public int estimateBufferSize(Picture frame) {
        // We assume it's the same size as raw image
        return frame.getWidth() * frame.getHeight() * 3;
    }

}
