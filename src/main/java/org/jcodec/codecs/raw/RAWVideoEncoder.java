package org.jcodec.codecs.raw;

import java.nio.ByteBuffer;

import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class RAWVideoEncoder extends VideoEncoder {

    @Override
    public EncodedFrame encodeFrame(Picture pic, ByteBuffer _out) {
        ByteBuffer dup = _out.duplicate();

        int width = pic.getWidth();
        int startY = 0;
        int startX = 0;
        int cropH = pic.getHeight();
        int cropW = pic.getWidth();

        Rect crop = pic.getCrop();
        if (crop != null) {
            width = pic.getWidth();
            startY = crop.getY();
            startX = crop.getX();
            cropH = crop.getHeight();
            cropW = crop.getWidth();
        }
        for (int plane = 0; plane < 3; plane++) {
            int pos = width * startY + startX;
            for (int y = 0; y < cropH; y++) {
                for (int x = 0; x < cropW; x++)
                    dup.put((byte) (pic.getPlaneData(plane)[pos + x] + 128));
                pos += width;
            }
            if (plane == 0) {
                width /= 2;
                startX /= 2;
                startY /= 2;
                cropH /= 2;
                cropW /= 2;
            }
        }
        dup.flip();
        return new EncodedFrame(dup, true);
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.YUV420 };
    }

    @Override
    public int estimateBufferSize(Picture frame) {
        return (frame.getCroppedWidth() * frame.getCroppedHeight() * 3) / 2;
    }

}
