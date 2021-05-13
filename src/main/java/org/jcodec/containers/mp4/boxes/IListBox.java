package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.Boxes;
import org.jcodec.containers.mp4.IBoxFactory;
import org.jcodec.containers.mp4.boxes.Box.AtomField;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IListBox extends Box {
    private static final String FOURCC = "ilst";
    private Map<Integer, List<Box>> values;
    private IBoxFactory factory;

    private static class LocalBoxes extends Boxes {
        //Initializing blocks are not supported by Javascript.
        LocalBoxes() {
            super();
            mappings.put(DataBox.fourcc(), DataBox.class);
        }
    }

    public IListBox(Header atom) {
        super(atom);
        factory = new SimpleBoxFactory(new LocalBoxes());
        values = new LinkedHashMap<Integer, List<Box>>();
    }

    public static IListBox createIListBox(Map<Integer, List<Box>> values) {
        IListBox box = new IListBox(Header.createHeader(FOURCC, 0));
        box.values = values;
        return box;
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

    @AtomField(idx=0)
    public Map<Integer, List<Box>> getValues() {
        return values;
    }

    protected void doWrite(ByteBuffer out) {
        for (Entry<Integer, List<Box>> entry : values.entrySet()) {
            ByteBuffer fork = out.duplicate();
            out.putInt(0);
            out.putInt(entry.getKey());
            for (Box box : entry.getValue()) {
                box.write(out);
            }
            fork.putInt(out.position() - fork.position());
        }
    }
    
    @Override
    public int estimateSize() {
        int sz = 8;
        for (Entry<Integer, List<Box>> entry : values.entrySet()) {
            for (Box box : entry.getValue()) {
                sz += 8 + box.estimateSize();
            }
        }
        return sz;
    }

    public static String fourcc() {
        return FOURCC;
    }
}
