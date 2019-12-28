package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BitRateBox extends FullBox {
    private int bufferSizeDB;
    private int maxBitrate;
    private int avgBitrate;

    public static BitRateBox createUriBox(int bufferSizeDB, int maxBitrate, int avgBitrate) {
        BitRateBox box = new BitRateBox(new Header(fourcc()));
        box.bufferSizeDB = bufferSizeDB;
        box.maxBitrate = maxBitrate;
        box.avgBitrate = avgBitrate;
        return box;
    }

    public BitRateBox(Header atom) {
        super(atom);
    }

    @Override
    public int estimateSize() {
        return 24;
    }

    public static String fourcc() {
        return "btrt";
    }
    
    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        bufferSizeDB = input.getInt();
        maxBitrate = input.getInt();
        avgBitrate = input.getInt();
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        out.putInt(bufferSizeDB);
        out.putInt(maxBitrate);
        out.putInt(avgBitrate);
    }

    public int getBufferSizeDB() {
        return bufferSizeDB;
    }

    public int getMaxBitrate() {
        return maxBitrate;
    }

    public int getAvgBitrate() {
        return avgBitrate;
    }
}
