package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.BoxUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EditsBox extends NodeBox {
    public static final String FOURCC = "edts";

    public static String fourcc() {
        return FOURCC;
    }

    public EditsBox(Header atom) {
        super(atom);
    }

    public static boolean isLookingLikeEdits(LeafBox leafBox) {
        ByteBuffer dup = leafBox.getData().duplicate();
        if (dup.remaining() > 16) {
            try {
                EditsBox parsed = (EditsBox) BoxUtil.parseBox(leafBox.getData(), Header.createHeader(FOURCC, 0),
                        BoxFactory.getDefault());
                EditListBox editListBox = NodeBox.findFirst(parsed, EditListBox.class, EditListBox.fourcc());
                if (editListBox != null) {
                    return true;
                }
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }
}
