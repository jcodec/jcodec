package org.jcodec.codecs.mpeg4.es;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DecoderConfig extends NodeDescriptor {
    private int objectType;
    private int bufSize;
    private int maxBitrate;
    private int avgBitrate;

    public DecoderConfig(int tag, int size) {
        super(tag, size);
    }
    
    public DecoderConfig(int objectType, int bufSize, int maxBitrate, int avgBitrate, Descriptor... children) {
        super(tag(), children);
        this.objectType = objectType;
        this.bufSize = bufSize;
        this.maxBitrate = maxBitrate;
        this.avgBitrate = avgBitrate;
    }

    protected void parse(InputStream input) throws IOException {

        objectType = input.read();
        input.read();
        bufSize = (input.read() << 16) | (int) ReaderBE.readInt16(input);
        maxBitrate = (int)ReaderBE.readInt32(input);
        avgBitrate = (int)ReaderBE.readInt32(input);

        super.parse(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(objectType);
        // flags (= Audiostream)
        out.write(0x15);
        out.write(bufSize >> 16);
        out.writeShort(bufSize & 0xffff);
        out.writeInt(maxBitrate);
        out.writeInt(avgBitrate);

        super.doWrite(out);
    }

    public static int tag() {
        return 0x4;
    }

    public int getObjectType() {
        return objectType;
    }

    public int getBufSize() {
        return bufSize;
    }

    public int getMaxBitrate() {
        return maxBitrate;
    }

    public int getAvgBitrate() {
        return avgBitrate;
    }
}
