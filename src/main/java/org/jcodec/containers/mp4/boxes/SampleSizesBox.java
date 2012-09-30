package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        defaultSize = (int)ReaderBE.readInt32(input);
        count = (int)ReaderBE.readInt32(input);

        if (defaultSize == 0) {
            sizes = new int[count];
            for (int i = 0; i < count; i++) {
                sizes[i] = (int)ReaderBE.readInt32(input);
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
    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt((int) defaultSize);

        if (defaultSize == 0) {
            out.writeInt(sizes.length);
            for (long size : sizes) {
                out.writeInt((int) size);
            }
        } else {
            out.writeInt((int)count);
        }
    }
}