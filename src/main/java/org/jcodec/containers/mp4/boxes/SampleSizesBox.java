package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class SampleSizesBox extends FullBox {
    private int defaultSize;
    private int count;
    private int[] sizes;
    
    public static String fourcc() {
        return "stsz";
    }

    public SampleSizesBox(int defaultSize, int count) {
        this();
        this.defaultSize = defaultSize;
        this.count = count;
    }
    
    public SampleSizesBox(int[] sizes) {
        this();
        this.sizes = sizes;
    }

    public SampleSizesBox() {
        super(new Header(fourcc()));
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        defaultSize = input.getInt();
        count = input.getInt();

        if (defaultSize == 0) {
            sizes = new int[count];
            for (int i = 0; i < count; i++) {
                sizes[i] = input.getInt();
            }
        }
    }

    public int getDefaultSize() {
        return defaultSize;
    }

    public int[] getSizes() {
        return sizes;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt((int) defaultSize);

        if (defaultSize == 0) {
            out.putInt(sizes.length);
            for (long size : sizes) {
                out.putInt((int) size);
            }
        } else {
            out.putInt((int)count);
        }
    }
}