package org.jcodec.codecs.pngawt;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.scale.AWTUtil;

/**
 * Video decoder wrapper to Java SE PNG functionality.
 * 
 * @author Stanislav Vitvitskyy
 */
public class PNGDecoder extends VideoDecoder {

    @Override
    public Picture8Bit decodeFrame8Bit(ByteBuffer data, byte[][] buffer) {
        try {
            BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
            return AWTUtil.fromBufferedImageRGB8Bit(rgb);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        try {
            BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
            return new VideoCodecMeta(new Size(rgb.getWidth(), rgb.getHeight()), ColorSpace.RGB);
        } catch (IOException e) {
            return null;
        }
    }
}
