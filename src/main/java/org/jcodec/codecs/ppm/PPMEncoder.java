package org.jcodec.codecs.ppm;

import java.io.PrintStream;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PPMEncoder {

    public Buffer encodeFrame(Picture picture) {
        if (picture.getColor() != ColorSpace.RGB)
            throw new IllegalArgumentException("Only RGB image can be stored in PPM");
        Buffer buffer = new Buffer(picture.getWidth() * picture.getHeight() * 3 + 200);
        new PrintStream(buffer.os()).println("P6 " + picture.getWidth() + " " + picture.getHeight() + " 255\n");

        int[][] data = picture.getData();
        for (int i = 0; i < picture.getWidth() * picture.getHeight() * 3; i += 3) {
            buffer.write(data[0][i + 2]);
            buffer.write(data[0][i + 1]);
            buffer.write(data[0][i]);
        }

        buffer.flip();

        return buffer;
    }
}