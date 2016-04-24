package org.jcodec.codecs.mpeg4.es;
import org.jcodec.common.Assert;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SL extends Descriptor {
    
    public SL() {
        super(tag(), 0);
    }

    protected void doWrite(ByteBuffer out) {
        out.put((byte)0x2);
    }

    protected static SL parse(ByteBuffer input, IDescriptorFactory factory) {
        Assert.assertEquals(0x2, input.get() & 0xff);
        return new SL();
    }

    public static int tag() {
        return 0x06;
    }
}
