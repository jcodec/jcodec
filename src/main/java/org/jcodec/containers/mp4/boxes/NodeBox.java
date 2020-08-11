package org.jcodec.containers.mp4.boxes;

import java.util.Arrays;
import java.util.Iterator;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mp4.IBoxFactory;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;

import java.lang.StringBuilder;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A node box
 * 
 * A box containing children, no data
 * 
 * @author The JCodec project
 * 
 */
public class NodeBox extends Box {
    protected List<Box> boxes;
    protected IBoxFactory factory;

    public NodeBox(Header atom) {
        super(atom);
        this.boxes = new LinkedList<Box>();
    }

    public void setFactory(IBoxFactory factory) {
        this.factory = factory;
    }
    
    public void parse(ByteBuffer input) {

        while (input.remaining() >= 8) {
            Box child = parseChildBox(input, factory);
            if (child != null)
                boxes.add(child);
        }
    }
    
    public static Box parseChildBox(ByteBuffer input, IBoxFactory factory) {
        ByteBuffer fork = input.duplicate();
        while (input.remaining() >= 4 && fork.getInt() == 0)
            input.getInt();
        if (input.remaining() < 4)
            return null;

        Box ret = null;
        Header childAtom = Header.read(input);
        if (childAtom != null && input.remaining() >= childAtom.getBodySize()) {
            ret = Box.parseBox(NIOUtils.read(input, (int) childAtom.getBodySize()), childAtom, factory);
        }
        return ret;
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public void add(Box box) {
        boxes.add(box);
    }

    protected void doWrite(ByteBuffer out) {
        for (Box box : boxes) {
            box.write(out);
        }
    }
    
    @Override
    public int estimateSize() {
        int total = 0;
        for (Box box : boxes) {
            total += box.estimateSize();
        }
        return total + Header.estimateHeaderSize(total);
    }

    public void addFirst(MovieHeaderBox box) {
        boxes.add(0, box);
    }

    public void replace(String fourcc, Box box) {
        removeChildren(new String[]{fourcc});
        add(box);
    }
    
    public void replaceBox(Box box) {
        removeChildren(new String[]{box.getFourcc()});
        add(box);
    }

    public void replaceBoxWith(LeafBox was, Box now) {
        for (ListIterator<Box> it = boxes.listIterator(); it.hasNext();) {
            Box box = it.next();
            if (box == was)
                it.set(now);
        }
    }

    protected void dump(StringBuilder sb) {
        sb.append("{\"tag\":\"" + header.getFourcc() + "\",");
        sb.append("\"boxes\": [");
        dumpBoxes(sb);
        sb.append("]");
        sb.append("}");
    }

    protected void dumpBoxes(StringBuilder sb) {
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).dump(sb);
            if (i < boxes.size() - 1)
                sb.append(",");
        }
    }

    public void removeChildren(String[] fourcc) {
        for (Iterator<Box> it = boxes.iterator(); it.hasNext();) {
            Box box = it.next();
            String fcc = box.getFourcc();
            for (int i = 0; i < fourcc.length; i++) {
                String cand = fourcc[i];
                if (cand.equals(fcc)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    public static Box doCloneBox(Box box, int approxSize, IBoxFactory bf) {
        ByteBuffer buf = ByteBuffer.allocate(approxSize);
        box.write(buf);
        ((java.nio.Buffer)buf).flip();
        return parseChildBox(buf, bf);
    }

    public static Box cloneBox(Box box, int approxSize, IBoxFactory bf) {
        return NodeBox.doCloneBox(box, approxSize, bf);
    }

    public static <T extends Box> T[] findDeep(Box box, Class<T> class1, String name) {
        List<T> storage = new ArrayList<T>();
        findDeepInner(box, class1, name, storage);
        return storage.toArray((T[]) Array.newInstance(class1, 0));
    }

    public static <T extends Box> void findDeepInner(Box box, Class<T> class1, String name, List<T> storage) {
        if (box == null)
            return;
        if (name.equals(box.getHeader().getFourcc())) {
            storage.add((T) box);
            return;
        }
        if (box instanceof NodeBox) {
            NodeBox nb = (NodeBox) box;
            for (Box candidate : nb.getBoxes()) {
                findDeepInner(candidate, class1, name, storage);
            }
        }
    }
    
    public static <T extends Box> T[] findAll(Box box, Class<T> class1, String path) {
        return findAllPath(box, class1, new String[] { path });
    }

    public static <T extends Box> T findFirst(NodeBox box, Class<T> clazz, String path) {
        return findFirstPath(box, clazz, new String[] { path });
    }

    public static <T extends Box> T findFirstPath(NodeBox box, Class<T> clazz, String[] path) {
        T[] result = (T[]) findAllPath(box, clazz, path);
        return result.length > 0 ? result[0] : null;
    }

    public static <T extends Box> T[] findAllPath(Box box, Class<T> class1, String[] path) {
        List<Box> result = new LinkedList<Box>();
        findBox(box, new ArrayList<String>(Arrays.asList(path)), result);
    
        for (ListIterator<Box> it = result.listIterator(); it.hasNext();) {
            Box next = it.next();
            if (next == null) {
                it.remove();
            } else if (!Platform.isAssignableFrom(class1, next.getClass())) {
                // Trying to reinterpret one box as the other
                try {
                    it.set(Box.asBox(class1, next));
                } catch (Exception e) {
                    Logger.warn("Failed to reinterpret box: " + next.getFourcc() + " as: " + class1.getName() + "."
                            + e.getMessage());
                    it.remove();
                }
            }
        }
        return result.toArray((T[]) Array.newInstance(class1, 0));
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
}
