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
    
    public MetaBox() {
        this(Header.createHeader(fourcc(), 0));
    }

    public Map<String, MetaValue> getKeyedMeta() {
        Map<String, MetaValue> result = new HashMap<String, MetaValue>();

        IListBox ilst = NodeBox.findFirst(this, IListBox.class, IListBox.fourcc());
        MdtaBox[] keys = NodeBox.findAllPath(this, MdtaBox.class, new String[] { KeysBox.fourcc(), MdtaBox.fourcc() });
        
        if (ilst == null || keys.length == 0)
            return result;
        
        for (Entry<Integer, DataBox> entry : ilst.getValues().entrySet()) {
            Integer index = entry.getKey();
            if (index == null)
                continue;
            DataBox db = entry.getValue();
            MetaValue value = MetaValue.createOtherWithLocale(db.getType(), db.getLocale(), db.getData());
            if (index > 0 && index <= keys.length) {
                result.put(keys[index - 1].getKey(), value);
            }
        }
        return result;
    }
    
    public Map<Integer, MetaValue> getItunesMeta() {
        Map<Integer, MetaValue> result = new HashMap<Integer, MetaValue>();

        IListBox ilst = NodeBox.findFirst(this, IListBox.class, IListBox.fourcc());
        
        if (ilst == null)
            return result;
        
        for (Entry<Integer, DataBox> entry : ilst.getValues().entrySet()) {
            Integer index = entry.getKey();
            if (index == null)
                continue;
            DataBox db = entry.getValue();
            MetaValue value = MetaValue.createOtherWithLocale(db.getType(), db.getLocale(), db.getData());
            result.put(index, value);
        }
        return result;
    }
    
    public void setKeyedMeta(Map<String, MetaValue> map) {
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
    
    public void setFourccMeta(Map<Integer, MetaValue> map) {
        Map<Integer, DataBox> data = new HashMap<Integer, DataBox>();
        for (Entry<Integer, MetaValue> entry : map.entrySet()) {
            MetaValue v = entry.getValue();
            data.put(entry.getKey(), new DataBox(v.getType(), v.getLocale(), v.getData()));
        }
        IListBox ilst = new IListBox(data);
        this.replaceBox(ilst);
    }

    public static String fourcc() {
        return FOURCC;
    }
}
