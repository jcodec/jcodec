package org.jcodec.codecs.mpeg4.es;

import static java.util.Arrays.asList;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
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
    private Collection<Descriptor> children = new ArrayList<Descriptor>();

    public NodeDescriptor(int tag, int size) {
        super(tag, size);
    }
    
    public NodeDescriptor(int tag) {
        super(tag);
    }

    public NodeDescriptor(int tag, Descriptor[] children) {
        super(tag);
        this.children.addAll(asList(children));
    }

    protected void doWrite(DataOutput out) throws IOException {
        for (Descriptor descr : children) {
            descr.write(out);
        }
    }

    public Collection<Descriptor> getChildren() {
        return children;
    }

    protected void parse(InputStream input) throws IOException {
        Descriptor d;
        do {
            d = Descriptor.read(input);
            if (d != null)
                children.add(d);
        } while (d != null);
    }
}