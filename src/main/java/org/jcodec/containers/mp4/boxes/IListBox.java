package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.Boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IListBox extends Box {
    private static final String FOURCC = "ilst";
    private Map<Integer, List<Box>> values = new LinkedHashMap<Integer, List<Box>>();
    private BoxFactory factory;

    private static class LocalBoxes extends Boxes {
        {
            mappings.put(DataBox.fourcc(), DataBox.class);
        }
    }

    public IListBox(Header atom) {
        super(atom);
        factory = new BoxFactory(new LocalBoxes());
    }

    public IListBox(Map<Integer, List<Box>> values) {
        this(Header.createHeader(FOURCC, 0));
        this.values = values;
    }

    public void parse(ByteBuffer input) {
        while (input.remaining() >= 4) {
            int size = input.getInt();
            ByteBuffer local = NIOUtils.read(input, size - 4);
            int index = local.getInt();
            List<Box> children = new ArrayList<Box>();
            values.put(index, children);
            while (local.hasRemaining()) {
                Header childAtom = Header.read(local);
                if (childAtom != null && local.remaining() >= childAtom.getBodySize()) {
                    Box box = Box.parseBox(NIOUtils.read(local, (int) childAtom.getBodySize()), childAtom, factory);
                    children.add(box);
                }
            }
        }
    }

    public Map<Integer, List<Box>> getValues() {
        return values;
    }

    protected void doWrite(ByteBuffer out) {
        Set<Entry<Integer, List<Box>>> entrySet = values.entrySet();
        for (Entry<Integer, List<Box>> entry : entrySet) {
            ByteBuffer fork = out.duplicate();
            out.putInt(0);
            out.putInt(entry.getKey());
            for (Box box : entry.getValue()) {
                box.write(out);
            }
            fork.putInt(out.position() - fork.position());
        }
    }

    public static String fourcc() {
        return FOURCC;
    }
}
