package org.jcodec.containers.mp4.boxes;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jcodec.common.Assert;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.ToJSON;

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
    private static final String GET_MODEL_FIELDS = "getModelFields";
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

    public static <T extends Box> T findFirst(NodeBox box, Class<T> clazz, String... path) {
        T[] result = (T[]) findAll(box, clazz, path);

        return result.length > 0 ? result[0] : null;
    }

    public static Box[] findAll(Box box, String... path) {
        return findAll(box, Box.class, path);
    }

    public static void findBox(Box root, List<String> path, Collection<Box> result) {

        if (path.size() > 0) {
            String head = path.remove(0);
            if (root instanceof NodeBox) {
                NodeBox nb = (NodeBox) root;
                for (Box candidate : nb.getBoxes()) {
                    if (head == null || head.equals(candidate.header.getFourcc())) {
                        findBox(candidate, path, result);
                    }
                }
            }
            path.add(0, head);
        } else {
            result.add(root);
        }
    }

    public static <T extends Box> T[] findAll(Box box, Class<T> class1, String... path) {
        List<Box> result = new LinkedList<Box>();
        List<String> tlist = new LinkedList<String>();
        for (String type : path) {
            tlist.add(type);
        }

        findBox(box, tlist, result);

        for (ListIterator<Box> it = result.listIterator(); it.hasNext();) {
            Box next = it.next();
            if (next == null) {
                it.remove();
            } else if (!class1.isAssignableFrom(next.getClass())) {
                // Trying to reinterpret one box as the other
                try {
                    it.set(Box.as(class1, next));
                } catch (Exception e) {
                    Logger.warn("Failed to reinterpret box: " + next.getFourcc() + " as: " + class1.getName() + "."
                            + e.getMessage());
                    it.remove();
                }
            }
        }
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
        sb.append("{\"tag\":\"" + header.getFourcc() + "\",");
        List<String> fields = new ArrayList<String>(0);
        collectModel(this.getClass(), fields);
        ToJSON.fieldsToJSON(this, sb, fields.toArray(new String[0]));
        sb.append("}");
    }

    protected void collectModel(Class claz, List<String> model) {
        if (Box.class == claz || !Box.class.isAssignableFrom(claz))
            return;

        collectModel(claz.getSuperclass(), model);

        try {
            Method method = claz.getDeclaredMethod(GET_MODEL_FIELDS, List.class);
            method.invoke(this, model);
        } catch (NoSuchMethodException e) {
            checkWrongSignature(claz);
            model.addAll(ToJSON.allFields(claz));
        } catch (Exception e) {
        }
    }

    private void checkWrongSignature(Class claz) {
        for (Method method : claz.getDeclaredMethods()) {
            if (method.getName().equals(GET_MODEL_FIELDS)) {
                Logger.warn("Class " + claz.getCanonicalName() + " contains 'getModelFields' of wrong signature.\n"
                        + "Did you mean to define 'protected void " + GET_MODEL_FIELDS + "(List<String> model) ?");
                break;
            }
        }
    }

    public static <T extends Box> T as(Class<T> class1, Box box) {
        try {
            T res = class1.getConstructor(Header.class).newInstance(box.getHeader());
            ByteBuffer buffer = ByteBuffer.allocate((int)box.getHeader().getBodySize());
            box.doWrite(buffer);
            buffer.flip();
            res.parse(buffer);
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T extends Box> T as(Class<T> class1, LeafBox box) {
        try {
            T res = class1.getConstructor(Header.class).newInstance(box.getHeader());
            res.parse(box.getData().duplicate());
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}