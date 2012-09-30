package org.jcodec.codecs.wav;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jcodec.algo.DataConvert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads integer samples from the wav file
 * 
 * @author The JCodec project
 */
public class WavInput {

    private InputStream is;
    private WavHeader header;
    private byte[] prevBuf;

    public WavInput(File file) throws IOException {
        is = new BufferedInputStream(new FileInputStream(file));
        header = WavHeader.read(is);
    }

    public int[] read(int samples) throws IOException {
        int bufLen = samples * (header.fmt.bitsPerSample >> 3);
        if (prevBuf == null || bufLen != prevBuf.length) {
            prevBuf = new byte[bufLen];
        }
        int read = is.read(prevBuf);
        if (read == -1)
            return null;
        int[] conv = DataConvert.fromByte(prevBuf, header.fmt.bitsPerSample, false);

        return read == bufLen ? conv : Arrays.copyOf(conv, read / (header.fmt.bitsPerSample >> 3));
    }

    public void close() throws IOException {
        is.close();
    }

    public WavHeader getHeader() {
        return header;
    }
}
