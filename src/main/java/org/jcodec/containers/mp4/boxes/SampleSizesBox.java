package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class SampleSizesBox extends FullBox {
    public SampleSizesBox(Header atom) {
        super(atom);
    }

    private int defaultSize;
    private int count;
    private int[] sizes;
    
    public static String fourcc() {
        return "stsz";
    }

    public static SampleSizesBox createSampleSizesBox(int defaultSize, int count) {
        SampleSizesBox stsz = new SampleSizesBox(new Header(fourcc()));
        stsz.defaultSize = defaultSize;
        stsz.count = count;
        return stsz;
    }

    public static SampleSizesBox createSampleSizesBox2(int[] sizes) {
        SampleSizesBox stsz = new SampleSizesBox(new Header(fourcc()));
        stsz.sizes = sizes;
        stsz.count = sizes.length;
        return stsz;
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
            out.putInt(count);
            for (int i = 0; i < sizes.length; i++) {
                long size = sizes[i];
                out.putInt((int) size);
            }
        } else {
            out.putInt((int)count);
        }
    }

    public void setSizes(int[] sizes) {
        this.sizes = sizes;
        this.count = sizes.length;
    }
}