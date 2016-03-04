package org.jcodec.codecs.h264.conformance;
import static org.jcodec.common.model.ColorSpace.YUV420;

import org.jcodec.common.model.Picture8Bit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * Reads raw yuv file
 * 
 * @author Jay Codec
 * 
 */
public class RawReader {
    private File rawFileName;
    private int width;
    private int height;

    private InputStream is;

    public RawReader(File rawFile, int width, int height) {
        this.rawFileName = rawFile;
        this.width = width;
        this.height = height;
    }

    public Picture8Bit readNextFrame8Bit() throws IOException {
        if (is == null) {
            is = new BufferedInputStream(new FileInputStream(rawFileName));
            if (is == null)
                return null;
        }

        return readFrame();
    }

    private Picture8Bit readFrame() throws IOException {

        int size = width * height;
        byte[] luma = new byte[size];
        byte[] cb = new byte[size >> 2];
        byte[] cr = new byte[size >> 2];

        byte first = (byte) (is.read() - 128);
        if (first == -1)
            return null;
        luma[0] = first;
        for (int i = 1; i < size; i++) {
            luma[i] = (byte) (is.read() - 128);
        }

        for (int i = 0; i < (size >> 2); i++) {
            cb[i] = (byte) (is.read() - 128);
        }

        for (int i = 0; i < (size >> 2); i++) {
            cr[i] = (byte) (is.read() - 128);
        }

        return Picture8Bit.createPicture8Bit(width, height, new byte[][] { luma, cb, cr }, YUV420);
    }

}
