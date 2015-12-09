package org.jcodec.codecs.mpeg4.es;

import java.nio.ByteBuffer;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class Descriptor {
    private int tag;
    private int size;
    private static DescriptorFactory factory = new DescriptorFactory();

    public Descriptor(int tag) {
        this(tag, 0);
    }

    public Descriptor(int tag, int size) {
        this.tag = tag;
        this.size = size;
    }

    public void write(ByteBuffer out) {
        ByteBuffer fork = out.duplicate();
        NIOUtils.skip(out, 5);
        doWrite(out);

        int length = out.position() - fork.position() - 5;
        fork.put((byte) tag);
        JCodecUtil.writeBER32(fork, length);
    }

    protected abstract void doWrite(ByteBuffer out);

    public static Descriptor read(ByteBuffer input) {
        if(input.remaining() < 2)
            return null;
        int tag = input.get() & 0xff;
        int size = JCodecUtil.readBER32(input);
        
        Class<? extends Descriptor> cls = factory.byTag(tag);
        Descriptor descriptor;
        try {
            descriptor = cls.getConstructor(Integer.TYPE, Integer.TYPE).newInstance(tag, (int) size);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        descriptor.parse(NIOUtils.read(input, size));
        return descriptor;
    }

    protected abstract void parse(ByteBuffer input);

    public static <T> T find(Descriptor es, Class<T> class1, int tag) {
        if (es.getTag() == tag)
            return (T) es;
        else {
            if (es instanceof NodeDescriptor) {
                for (Descriptor descriptor : ((NodeDescriptor) es).getChildren()) {
                    T res = find(descriptor, class1, tag);
                    if (res != null)
                        return res;
                }
            }
        }
        return null;
    }

    private int getTag() {
        return tag;
    }
}
