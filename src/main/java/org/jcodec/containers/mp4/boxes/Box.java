package org.jcodec.containers.mp4.boxes;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections15.Predicate;
import org.jcodec.common.NIOUtils;
import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An MP4 file struncture (box).
 * 
 * @author The JCodec project
 * 
 */
public abstract class Box {
    protected Header header;

    public Box(Header header) {
        this.header = header;
    }

    public Box(Box other) {
        this.header = other.header;
    }

    public Header getHeader() {
        return header;
    }

    public abstract void parse(ByteBuffer buf);

    public static Box findFirst(NodeBox box, String... path) {
        return findFirst(box, Box.class, path);
    }

    public static <T> T findFirst(NodeBox box, Class<T> clazz, String... path) {
        T[] result = (T[]) findAll(box, clazz, path);

        return result.length > 0 ? result[0] : null;
    }

    public static Box[] findAll(Box box, String... path) {
        return findAll(box, Box.class, path);
    }

    private static void findSub(Box box, List<String> path, Collection<Box> result) {

        if (path.size() > 0) {
            String head = path.remove(0);
            if (box instanceof NodeBox) {
                NodeBox nb = (NodeBox) box;
                for (Box candidate : nb.getBoxes()) {
                    if (head == null || head.equals(candidate.header.getFourcc())) {
                        findSub(candidate, path, result);
                    }
                }
            }
            path.add(0, head);
        } else {
            result.add(box);
        }
    }

    public static <T> T[] findAll(Box box, Class<T> class1, String... path) {
        List<Box> result = new LinkedList<Box>();
        List<String> tlist = new LinkedList<String>();
        for (String type : path) {
            tlist.add(type);
        }
        findSub(box, tlist, result);
        return result.toArray((T[]) Array.newInstance(class1, 0));
    }

    public void write(ByteBuffer buf) {
        ByteBuffer dup = buf.duplicate();
        NIOUtils.skip(buf, 8);
        doWrite(buf);

        header.setBodySize(buf.position() - dup.position() - 8);
        Assert.assertEquals(header.headerSize(), 8);
        header.write(dup);
    }

    public static Predicate<Box> not(final String type) {
        return new Predicate<Box>() {
            public boolean evaluate(Box object) {
                return !object.getHeader().getFourcc().equals(type);
            }
        };
    }

    protected abstract void doWrite(ByteBuffer out);

    public String getFourcc() {
        return header.getFourcc();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        dump(sb);
        return sb.toString();

    }

    protected void dump(StringBuilder sb) {
        sb.append("'" + header.getFourcc() + "'");
    }

    public static <T extends Box> T as(Class<T> class1, LeafBox box) {
        try {
            T res = class1.getConstructor(Header.class).newInstance(box.getHeader());
            res.parse(box.getData());
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}