package org.jcodec.containers.mp4.boxes;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.io.IOUtils;

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

    public abstract void parse(InputStream input) throws IOException;

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

    public void write(DataOutput out) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        doWrite(new DataOutputStream(bout));
        byte[] bytes = bout.toByteArray();

        header.setBodySize(bytes.length);
        header.write(out);
        out.write(bytes);
    }

    public void write(File dst) throws IOException {
        DataOutputStream dout = null;
        try {
            dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dst)));
            write(dout);
        } finally {
            IOUtils.closeQuietly(dout);
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        throw new RuntimeException(header.getFourcc() + " not implemented, must override");
    }

    public static Predicate<Box> not(final String type) {
        return new Predicate<Box>() {
            public boolean evaluate(Box object) {
                return !object.getHeader().getFourcc().equals(type);
            }
        };
    }

    public Box cloneBox() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            write(new DataOutputStream(baos));
            return NodeBox.parseChildBox(new ByteArrayInputStream(baos.toByteArray()), BoxFactory.getDefault());
        } catch (IOException e) {
        }
        return null;
    }

    public long calcSize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(new DataOutputStream(baos));
        } catch (IOException e) {
        }
        return baos.size();
    }

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
}