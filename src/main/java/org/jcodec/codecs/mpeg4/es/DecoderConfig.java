package org.jcodec.codecs.mpeg4.es;

import java.nio.ByteBuffer;

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

    protected void parse(ByteBuffer input) {

        objectType = input.get() & 0xff;
        input.get();
        bufSize = ((input.get() & 0xff) << 16) | (input.getShort() & 0xffff);
        maxBitrate = input.getInt();
        avgBitrate = input.getInt();

        super.parse(input);
    }

    protected void doWrite(ByteBuffer out) {
        out.put((byte) objectType);
        // flags (= Audiostream)
        out.put((byte) 0x15);
        out.put((byte) (bufSize >> 16));
        out.putShort((short) bufSize);
        out.putInt(maxBitrate);
        out.putInt(avgBitrate);

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
