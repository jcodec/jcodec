package org.jcodec.codecs.raw;

import java.nio.ByteBuffer;

import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class RAWVideoDecoder extends VideoDecoder {
    private int width;
    private int height;

    public RAWVideoDecoder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Picture8Bit decodeFrame8Bit(ByteBuffer data, byte[][] buffer) {
        Picture8Bit create = Picture8Bit.createPicture8Bit(width, height, buffer, ColorSpace.YUV420);

        ByteBuffer pix = data.duplicate();
        copy(pix, create.getPlaneData(0), width * height);
        copy(pix, create.getPlaneData(1), width * height / 4);
        copy(pix, create.getPlaneData(2), width * height / 4);

        return create;
    }

    void copy(ByteBuffer b, byte[] ii, int size) {
        for (int i = 0; b.hasRemaining() && i < size; i++) {
            ii[i] = (byte) ((b.get() & 0xff) - 128);
        }
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        return new VideoCodecMeta(new Size(width, height), ColorSpace.YUV420);
    }
}
