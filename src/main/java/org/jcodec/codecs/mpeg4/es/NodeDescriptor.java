package org.jcodec.codecs.mpeg4.es;

import java.nio.ByteBuffer;
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

    public static <T> T findByTag(Descriptor es, int tag) {
        if (es.getTag() == tag)
            return (T) es;
        else {
            if (es instanceof NodeDescriptor) {
                for (Descriptor descriptor : ((NodeDescriptor) es).getChildren()) {
                    T res = findByTag(descriptor, tag);
                    if (res != null)
                        return res;
                }
            }
        }
        return null;
    }
}