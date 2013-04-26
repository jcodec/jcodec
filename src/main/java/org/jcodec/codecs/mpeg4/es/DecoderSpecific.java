package org.jcodec.codecs.mpeg4.es;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DecoderSpecific extends UnknownDescriptor {
    
    public DecoderSpecific(byte[] data) {
        super(tag(), data);
    }

    public DecoderSpecific(int tag, int size) {
        super(tag, size);
    }
    
    public static int tag() {
        return 0x5;
    }
}
