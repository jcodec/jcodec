package org.jcodec.codecs.pngawt;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.common.Codec;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.scale.AWTUtil;

/**
 * Video decoder wrapper to Java SE PNG functionality.
 * 
 * @author Stanislav Vitvitskyy
 */
public class PNGDecoder extends VideoDecoder {

    @Override
    public Picture decodeFrame(ByteBuffer data, byte[][] buffer) {
        try {
            BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
            return AWTUtil.fromBufferedImageRGB(rgb);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        try {
            BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
            return VideoCodecMeta.createSimpleVideoCodecMeta(Codec.PNG, null, new Size(rgb.getWidth(), rgb.getHeight()),
                    ColorSpace.RGB);
        } catch (IOException e) {
            return null;
        }
    }
}
