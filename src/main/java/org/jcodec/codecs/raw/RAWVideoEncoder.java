package org.jcodec.codecs.raw;

import java.nio.ByteBuffer;

import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

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

        ColorSpace color = pic.getColor();
        for (int plane = 0; plane < color.nComp; plane++) {
            int width = pic.getWidth() >> color.compWidth[plane];
            int startX = pic.getStartX();
            int startY = pic.getStartY();
            int cropW = pic.getCroppedWidth() >> color.compWidth[plane];
            int cropH = pic.getCroppedHeight() >> color.compHeight[plane];

            int pos = width * startY + startX;
            for (int y = 0; y < cropH; y++) {
                for (int x = 0; x < cropW; x++)
                    dup.put((byte) (pic.getPlaneData(plane)[pos + x] + 128));
                pos += width;
            }
        }
        dup.flip();
        return new EncodedFrame(dup, true);
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return null;
    }

    @Override
    public int estimateBufferSize(Picture frame) {
        int fullPlaneSize = frame.getWidth() * frame.getCroppedHeight();
        ColorSpace color = frame.getColor();
        int totalSize = 0;
        for (int i = 0; i < color.nComp; i++) {
            totalSize += (fullPlaneSize >> color.compWidth[i]) >> color.compHeight[i];
        }
        return totalSize;
    }

}
