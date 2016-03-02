package org.jcodec.containers.mp4;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.NodeBox;

import java.nio.ByteBuffer;

public class BoxUtil {

    public static Box parseBox(ByteBuffer input, Header childAtom, IBoxFactory factory) {
        Box box = factory.newBox(childAtom);

        if (childAtom.getBodySize() < Box.MAX_BOX_SIZE) {
            box.parse(input);
            return box;
        } else {
            return new Box.LeafBox(Header.createHeader("free", 8));
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

    public static <T extends Box> T as(Class<T> class1, Box.LeafBox box) {
        try {
            T res = class1.getConstructor(Header.class).newInstance(box.getHeader());
            res.parse(box.getData().duplicate());
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsBox(NodeBox box, String path) {
        Box b = NodeBox.findFirstPath(box, Box.class, new String[] { path });
        return b != null;
    }

    public static boolean containsBox2(NodeBox box, String path1, String path2) {
        Box b = NodeBox.findFirstPath(box, Box.class, new String[] { path1, path2 });
        return b != null;
    }

}
