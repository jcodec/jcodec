package org.jcodec.codecs.mpeg4.es;
import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.io.NIOUtils;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class Descriptor {
    private int _tag;
    private int size;
    protected IDescriptorFactory factory;
    
    public Descriptor(int tag, int size) {
        this._tag = tag;
        this.size = size;
    }

    public void write(ByteBuffer out) {
        ByteBuffer fork = out.duplicate();
        NIOUtils.skip(out, 5);
        doWrite(out);

        int length = out.position() - fork.position() - 5;
        fork.put((byte) _tag);
        JCodecUtil2.writeBER32(fork, length);
    }

    protected abstract void doWrite(ByteBuffer out);

    int getTag() {
        return _tag;
    }
    
    public static Descriptor read(ByteBuffer input, IDescriptorFactory factory) {
        if(input.remaining() < 2)
            return null;
        int tag = input.get() & 0xff;
        int size = JCodecUtil2.readBER32(input);
        
        Class<? extends Descriptor> cls = factory.byTag(tag);
        Descriptor descriptor;
        try {
            Method method = cls.getDeclaredMethod("parse", ByteBuffer.class, IDescriptorFactory.class);
            descriptor = (Descriptor)method.invoke(null, NIOUtils.read(input, size), factory);
            descriptor.setFactory(factory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return descriptor;
    }

    private void setFactory(IDescriptorFactory factory) {
        this.factory = factory;
    }
}
