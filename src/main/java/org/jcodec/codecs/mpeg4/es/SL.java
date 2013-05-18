package org.jcodec.codecs.mpeg4.es;

import java.nio.ByteBuffer;

import org.jcodec.common.Assert;

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

    protected void doWrite(ByteBuffer out) {
        out.put((byte)0x2);
    }

    protected void parse(ByteBuffer input) {
        Assert.assertEquals(0x2, input.get() & 0xff);
    }

    public static int tag() {
        return 0x06;
    }
}
