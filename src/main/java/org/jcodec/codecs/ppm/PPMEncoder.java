package org.jcodec.codecs.ppm;
import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

import js.lang.IllegalArgumentException;
import js.nio.ByteBuffer;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PPMEncoder {

    public ByteBuffer encodeFrame8Bit(Picture8Bit picture) {
        if (picture.getColor() != ColorSpace.RGB)
            throw new IllegalArgumentException("Only RGB image can be stored in PPM");
        ByteBuffer buffer = ByteBuffer.allocate(picture.getWidth() * picture.getHeight() * 3 + 200);
        buffer.putArr(JCodecUtil2.asciiString("P6 " + picture.getWidth() + " " + picture.getHeight() + " 255\n"));

        byte[][] data = picture.getData();
        for (int i = 0; i < picture.getWidth() * picture.getHeight() * 3; i += 3) {
            buffer.put((byte) (data[0][i + 2] + 128));
            buffer.put((byte) (data[0][i + 1] + 128));
            buffer.put((byte) (data[0][i] + 128));
        }

        buffer.flip();

        return buffer;
    }
}