package org.jcodec.codecs.mpeg4.es;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class NodeDescriptor extends Descriptor {
    private Collection<Descriptor> children;

    public NodeDescriptor(int tag, Collection<Descriptor> children) {
        super(tag, 0);
        this.children = children;
    }

    protected void doWrite(ByteBuffer out) {
        for (Descriptor descr : children) {
            descr.write(out);
        }
    }

    public Collection<Descriptor> getChildren() {
        return children;
    }

    protected static NodeDescriptor parse(ByteBuffer input, IDescriptorFactory factory) {
        Collection<Descriptor> children = new ArrayList<Descriptor>();
        Descriptor d;
        do {
            d = Descriptor.read(input, factory);
            if (d != null)
                children.add(d);
        } while (d != null);
        return new NodeDescriptor(0, children);
    }

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
}