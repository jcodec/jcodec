package org.jcodec.codecs.mpeg4.es;

import static java.util.Arrays.asList;

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
    
    public NodeDescriptor(int tag, Descriptor[] children) {
        super(tag, 0);
        this.children = new ArrayList<Descriptor>();
        this.children.addAll(asList(children));
    }

    protected void doWrite(ByteBuffer out) {
        for (Descriptor descr : children) {
            descr.write(out);
        }
    }

    public Collection<Descriptor> getChildren() {
        return children;
    }

    protected void parse(ByteBuffer input) {
        Descriptor d;
        do {
            d = Descriptor.read(input);
            if (d != null)
                children.add(d);
        } while (d != null);
    }
}