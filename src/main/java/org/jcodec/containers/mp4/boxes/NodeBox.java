package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.jcodec.common.io.WindowInputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A node box
 * 
 * A box containing children, no data
 * 
 * @author Jay Codec
 * 
 */
public class NodeBox extends Box {
    private static final int MAX_BOX_SIZE = 128 * 1024 * 1024;
    protected List<Box> boxes = new LinkedList<Box>();
    protected BoxFactory factory = BoxFactory.getDefault();

    public NodeBox(Header atom) {
        super(atom);
    }

    public NodeBox(NodeBox other) {
        super(other);
        this.boxes = other.boxes;
        this.factory = other.factory;
    }

    public void parse(InputStream input) throws IOException {

        Box box;
        while ((box = parseChildBox(input, factory)) != null)
            boxes.add(box);
    }

    protected static Box parseChildBox(InputStream input, BoxFactory factory) throws IOException {
        Header childAtom = Header.read(input);
        
        if (childAtom == null)
            return null;

        WindowInputStream wi = new WindowInputStream(input, childAtom.getBodySize());
        return parseBox(wi, childAtom, factory);
    }

    public static Box newBox(Header header, BoxFactory factory) {
        Class<? extends Box> claz = factory.toClass(header.getFourcc());
        if (claz == null)
            return new LeafBox(header);
        try {
            try {
                return claz.getConstructor(Header.class).newInstance(header);
            } catch (NoSuchMethodException e) {
                return claz.newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Box parseBox(InputStream input, Header childAtom, BoxFactory factory) throws IOException {
        Box box = newBox(childAtom, factory);
        
        if (childAtom.getBodySize() < MAX_BOX_SIZE) {
            WindowInputStream window = new WindowInputStream(input, (int) childAtom.getBodySize());
            box.parse(window);
            window.skipRemaining();
            return box;
        } else {
            return new LeafBox(new Header("free", 8));
        }
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public void add(Box box) {
        boxes.add(box);
    }

    protected void doWrite(DataOutput out) throws IOException {
        for (Box box : boxes) {
            box.write(out);
        }
    }

    public void addFirst(MovieHeaderBox box) {
        boxes.add(0, box);
    }

    public void filter(Predicate<Box> predicate) {
        CollectionUtils.filter(boxes, predicate);
    }

    public void replace(String fourcc, Box box) {
        filter(not(fourcc));
        add(box);
    }

    public void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": {\n");

        dumpBoxes(sb);
        sb.append("\n}");
    }

    protected void dumpBoxes(StringBuilder sb) {
        StringBuilder sb1 = new StringBuilder();
        Iterator<Box> iterator = boxes.iterator();
        while (iterator.hasNext()) {
            iterator.next().dump(sb1);
            if (iterator.hasNext())
                sb1.append(",\n");
        }
        sb.append(sb1.toString().replaceAll("([^\n]*)\n", "  $1\n"));
    }
}