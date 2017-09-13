package org.jcodec.containers.mp4.boxes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MetaBox extends NodeBox {

    private static final String FOURCC = "meta";

    public MetaBox(Header atom) {
        super(atom);
    }

    public Map<String, MetaValue> getMeta() {
        Map<String, MetaValue> result = new HashMap<String, MetaValue>();

        IListBox ilst = NodeBox.findFirst(this, IListBox.class, IListBox.fourcc());
        MdtaBox[] keys = NodeBox.findAllPath(this, MdtaBox.class, new String[] { KeysBox.fourcc(), MdtaBox.fourcc() });
        for (Entry<Integer, DataBox> entry : ilst.getValues().entrySet()) {
            DataBox db = entry.getValue();
            MetaValue value = MetaValue.createOtherWithLocale(db.getType(), db.getLocale(), db.getData());
            Integer index = entry.getKey();
            if (index != null && index > 0 && index <= keys.length) {
                result.put(keys[index - 1].getKey(), value);
            }
        }

        return result;
    }

    public void setMeta(Map<String, MetaValue> map) {

        KeysBox keys = new KeysBox();
        Map<Integer, DataBox> data = new HashMap<Integer, DataBox>();
        int i = 1;
        for (Entry<String, MetaValue> entry : map.entrySet()) {
            keys.add(new MdtaBox(entry.getKey()));
            MetaValue v = entry.getValue();
            data.put(i, new DataBox(v.getType(), v.getLocale(), v.getData()));
            ++i;
        }
        IListBox ilst = new IListBox(data);
        this.replaceBox(keys);
        this.replaceBox(ilst);
    }

    public static String fourcc() {
        return FOURCC;
    }
}
