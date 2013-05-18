package org.jcodec.common.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.IOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Interleaves 2 or more single channel wave files into a multichannel wav
 * 
 * @author The JCodec project
 * 
 */
public class WavMerge {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("wavmerge <output wav> <input wav> .... <input wav>");
            System.exit(-1);
        }
        File out = new File(args[0]);
        File[] ins = new File[args.length - 1];
        for (int i = 1; i < args.length; i++)
            ins[i - 1] = new File(args[i]);
        merge(out, ins);
    }

    public static void merge(File result, File... src) throws IOException {

        List<InputStream> inputs = new ArrayList<InputStream>();
        BufferedOutputStream out = null;
        List<WavHeader> headers = new ArrayList<WavHeader>();
        try {
            int sampleSize = -1;
            long dataSize = -1;
            for (File wav : src) {
                BufferedInputStream is = new BufferedInputStream(new FileInputStream(wav));
                inputs.add(is);
                WavHeader hdr = WavHeader.read(is);
                if (sampleSize != -1 && sampleSize != hdr.fmt.bitsPerSample)
                    throw new RuntimeException("Input files have different sample sizes");
                if (dataSize != -1 && dataSize != hdr.dataSize)
                    throw new RuntimeException("Input files have different duration");
                sampleSize = hdr.fmt.bitsPerSample;
                dataSize = hdr.dataSize;
                headers.add(hdr);
            }
            int ss = sampleSize >> 3;
            int nSamples = (int) (dataSize / ss);
            byte[] sample = new byte[ss];

            WavHeader newHeader = WavHeader.multiChannelWav(headers.toArray(new WavHeader[0]));
            out = new BufferedOutputStream(new FileOutputStream(result));
            newHeader.write(out);

            for (int i = 0; i < nSamples; i++) {
                for (InputStream is : inputs) {
                    is.read(sample);
                    out.write(sample);
                }
            }

        } finally {
            IOUtils.closeQuietly(out);
            for (InputStream inputStream : inputs) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }
}
