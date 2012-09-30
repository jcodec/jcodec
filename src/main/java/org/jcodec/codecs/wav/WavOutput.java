package org.jcodec.codecs.wav;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jcodec.algo.DataConvert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs integer samples into wav file
 * 
 * @author The JCodec project
 */
public class WavOutput {

    private BufferedOutputStream out;
    private WavHeader header;

    public WavOutput(File f, WavHeader wav) throws IOException {
        out = new BufferedOutputStream(new FileOutputStream(f));
        wav.write(out);
        this.header = wav;
    }

    public void write(int[] samples) throws IOException {

        out.write(DataConvert.toByte(samples, header.fmt.bitsPerSample, false));
    }

    public void close() throws IOException {
        out.close();
    }

}
