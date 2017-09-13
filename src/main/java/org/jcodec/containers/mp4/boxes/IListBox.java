package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.HashMap;
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
    private Map<Integer, DataBox> values = new HashMap<Integer, DataBox>();
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

    public IListBox(Map<Integer, DataBox> values) {
        this(Header.createHeader(FOURCC, 0));
        this.values = values;
    }

    public void parse(ByteBuffer input) {
        while (input.remaining() >= 4) {
            int size = input.getInt();
            int index = input.getInt();
            Header childAtom = Header.read(input);
            if (childAtom != null && input.remaining() >= childAtom.getBodySize()) {
                values.put(index, (DataBox) Box.parseBox(NIOUtils.read(input, (int) childAtom.getBodySize()), childAtom,
                        factory));
            }
        }
    }

    public Map<Integer, DataBox> getValues() {
        return values;
    }

    protected void doWrite(ByteBuffer out) {
        Set<Entry<Integer, DataBox>> entrySet = values.entrySet();
        for (Entry<Integer, DataBox> entry : entrySet) {
            ByteBuffer fork = out.duplicate();
            out.putInt(0);
            out.putInt(entry.getKey());
            entry.getValue().write(out);
            fork.putInt(out.position() - fork.position());
        }
    }

    public static String fourcc() {
        return FOURCC;
    }
}
