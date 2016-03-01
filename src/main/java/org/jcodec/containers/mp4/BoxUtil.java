package org.jcodec.containers.mp4;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jcodec.common.StringUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.boxes.WaveExtension;

public class BoxUtil {

    public static Box parseBox(ByteBuffer input, Header childAtom, IBoxFactory factory) {
        Box box = factory.newBox(childAtom);

        if (childAtom.getBodySize() < NodeBox.MAX_BOX_SIZE) {
            box.parse(input);
            return box;
        } else {
            return new LeafBox(Header.createHeader("free", 8));
        }
    }


    public static Box parseChildBox(ByteBuffer input, IBoxFactory factory) {
        ByteBuffer fork = input.duplicate();
        while (input.remaining() >= 4 && fork.getInt() == 0)
            input.getInt();
        if (input.remaining() < 4)
            return null;

        Header childAtom = Header.read(input);
        if (childAtom != null && input.remaining() >= childAtom.getBodySize())
            return parseBox(NIOUtils.read(input, (int) childAtom.getBodySize()), childAtom, factory);
        else
            return null;
    }

    public static <T extends Box> T findFirst(NodeBox box, Class<T> clazz, String path) {
        return BoxUtil.findFirstPath(box, clazz, new String[] { path });
    }

    public static <T extends Box> T findFirstPath(NodeBox box, Class<T> clazz, String[] path) {
        T[] result = (T[]) BoxUtil.findAllPath(box, clazz, path);
        return result.length > 0 ? result[0] : null;
    }

    public static <T extends Box> T[] findAll(Box box, Class<T> class1, String path) {
        return BoxUtil.findAllPath(box, class1, new String[] { path });
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

    public static <T extends Box> T[] findAllPath(Box box, Class<T> class1, String[] path) {
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

    public static <T extends Box> T as(Class<T> class1, LeafBox box) {
        try {
            T res = class1.getConstructor(Header.class).newInstance(box.getHeader());
            res.parse(box.getData().duplicate());
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsBox(NodeBox box, String path) {
        Box b = findFirstPath(box, Box.class, new String[] { path });
        return b != null;
    }

    public static boolean containsBox2(NodeBox box, String path1, String path2) {
        Box b = findFirstPath(box, Box.class, new String[] { path1, path2 });
        return b != null;
    }

    public static String[] path(String path) {
        return StringUtils.splitC(path, '.');
    }

}
