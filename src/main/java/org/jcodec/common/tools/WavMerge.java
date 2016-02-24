package org.jcodec.common.tools;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.AudioUtil;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;

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

    public static void merge(File result, File[] src) throws IOException {

        WritableByteChannel out = null;
        ReadableByteChannel[] inputs = new ReadableByteChannel[src.length];
        WavHeader[] headers = new WavHeader[src.length];
        ByteBuffer[] ins = new ByteBuffer[src.length];
        try {
            int sampleSize = -1;
            for (int i = 0; i < src.length; i++) {
                inputs[i] = NIOUtils.readableChannel(src[i]);
                WavHeader hdr = WavHeader.read(inputs[i]);
                if (sampleSize != -1 && sampleSize != hdr.fmt.bitsPerSample)
                    throw new RuntimeException("Input files have different sample sizes");
                sampleSize = hdr.fmt.bitsPerSample;
                headers[i] = hdr;
                ins[i] = ByteBuffer.allocate(hdr.getFormat().framesToBytes(4096));
            }
            ByteBuffer outb = ByteBuffer.allocate(headers[0].getFormat().framesToBytes(4096) * src.length);

            WavHeader newHeader = WavHeader.multiChannelWav(headers);
            out = NIOUtils.writableChannel(result);
            newHeader.write(out);

            for (boolean readOnce = true;;) {
                readOnce = false;
                for (int i = 0; i < ins.length; i++) {
                    if (inputs[i] != null) {
                        ins[i].clear();
                        if (inputs[i].read(ins[i]) == -1) {
                            NIOUtils.closeQuietly(inputs[i]);
                            inputs[i] = null;
                        } else
                            readOnce = true;
                        ins[i].flip();
                    }
                }
                if (!readOnce)
                    break;
                outb.clear();
                AudioUtil.interleave(headers[0].getFormat(), ins, outb);
                outb.flip();
                out.write(outb);
            }
        } finally {
            IOUtils.closeQuietly(out);
            for (ReadableByteChannel inputStream : inputs) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }
}
