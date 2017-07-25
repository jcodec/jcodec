package org.jcodec.codecs.h264.conformance;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture8Bit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.jcodec.common.model.ColorSpace.YUV420;

/**
 * 
 * Reads raw yuv file
 * 
 * @author Stan Vitvitskiy
 * 
 */
public class RawReader8Bit {
    private File rawFileName;
    private int width;
    private int height;
    private SeekableByteChannel ch;
    private ByteBuffer readBuf;

    public RawReader8Bit(File rawFile, int width, int height) {
        this.rawFileName = rawFile;
        this.width = width;
        this.height = height;
        this.readBuf = ByteBuffer.allocate(3 * width * height / 2);
    }

    public Picture8Bit readNextFrame() throws IOException {
        if (ch == null) {
            ch = NIOUtils.readableChannel(rawFileName);
        }

        return readFrame();
    }

    private Picture8Bit readFrame() throws IOException {
        int size = width * height;
        byte[] luma = new byte[size];
        byte[] cb = new byte[size >> 2];
        byte[] cr = new byte[size >> 2];

        readBuf.clear();
        int read = ch.read(readBuf);
        readBuf.flip();

        if (read != 3 * width * height / 2)
            return null;

        for (int i = 0; i < size; i++) {
            luma[i] = (byte) ((readBuf.get() & 0xff) - 128);
        }

        for (int i = 0; i < (size >> 2); i++) {
            cb[i] = (byte) ((readBuf.get() & 0xff) - 128);
        }

        for (int i = 0; i < (size >> 2); i++) {
            cr[i] = (byte) ((readBuf.get() & 0xff) - 128);
        }

        return Picture8Bit.createPicture8Bit(width, height, new byte[][] { luma, cb, cr }, YUV420);
    }
}
