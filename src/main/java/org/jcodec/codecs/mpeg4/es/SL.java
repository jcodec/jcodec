package org.jcodec.codecs.mpeg4.es;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SL extends Descriptor {
    
    public SL(int tag, int size) {
        super(tag, size);
    }
    
    public SL() {
        super(tag());
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(0x2);
    }

    protected void parse(InputStream input) throws IOException {
        Assert.assertEquals(0x2, input.read());
    }

    public static int tag() {
        return 0x06;
    }
}
