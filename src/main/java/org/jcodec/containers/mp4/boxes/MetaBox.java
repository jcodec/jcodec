package org.jcodec.containers.mp4.boxes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
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
        Map<String, MetaValue> result = new LinkedHashMap<String, MetaValue>();

        IListBox ilst = NodeBox.findFirst(this, IListBox.class, IListBox.fourcc());
        MdtaBox[] keys = NodeBox.findAllPath(this, MdtaBox.class, new String[] { KeysBox.fourcc(), MdtaBox.fourcc() });

        if (ilst == null || keys.length == 0)
            return result;

        for (Entry<Integer, List<Box>> entry : ilst.getValues().entrySet()) {
            Integer index = entry.getKey();
            if (index == null)
                continue;
            DataBox db = getDataBox(entry.getValue());
            if (db == null)
                continue;
            MetaValue value = MetaValue.createOtherWithLocale(db.getType(), db.getLocale(), db.getData());
            if (index > 0 && index <= keys.length) {
                result.put(keys[index - 1].getKey(), value);
            }
        }
        return result;
    }

    private DataBox getDataBox(List<Box> value) {
        for (Box box : value) {
            if (box instanceof DataBox) {
                return (DataBox) box;
            }
        }
        return null;
    }

    public Map<Integer, MetaValue> getItunesMeta() {
        Map<Integer, MetaValue> result = new LinkedHashMap<Integer, MetaValue>();

        IListBox ilst = NodeBox.findFirst(this, IListBox.class, IListBox.fourcc());

        if (ilst == null)
            return result;

        for (Entry<Integer, List<Box>> entry : ilst.getValues().entrySet()) {
            Integer index = entry.getKey();
            if (index == null)
                continue;
            DataBox db = getDataBox(entry.getValue());
            if (db == null)
                continue;
            MetaValue value = MetaValue.createOtherWithLocale(db.getType(), db.getLocale(), db.getData());
            result.put(index, value);
        }
        return result;
    }

    public void setKeyedMeta(Map<String, MetaValue> map) {
        if (map.isEmpty())
            return;
        KeysBox keys = new KeysBox();
        Map<Integer, List<Box>> data = new LinkedHashMap<Integer, List<Box>>();
        int i = 1;
        for (Entry<String, MetaValue> entry : map.entrySet()) {
            keys.add(new MdtaBox(entry.getKey()));
            MetaValue v = entry.getValue();
            List<Box> children = new ArrayList<Box>();
            children.add(new DataBox(v.getType(), v.getLocale(), v.getData()));
            data.put(i, children);
            ++i;
        }
        IListBox ilst = new IListBox(data);
        this.replaceBox(keys);
        this.replaceBox(ilst);
    }

    public void setItunesMeta(Map<Integer, MetaValue> map) {
        if (map.isEmpty())
            return;
        IListBox ilst = NodeBox.findFirst(this, IListBox.class, IListBox.fourcc());
        Map<Integer, List<Box>> data;
        if (ilst == null) {
            data = new LinkedHashMap<Integer, List<Box>>();
        } else {
            data = ilst.getValues();

            for (Entry<Integer, List<Box>> entry : data.entrySet()) {
                int index = entry.getKey();
                MetaValue v = map.get(index);
                if (v != null) {
                    DataBox dataBox = new DataBox(v.getType(), v.getLocale(), v.getData());
                    dropChildBox(entry.getValue(), DataBox.fourcc());
                    entry.getValue().add(dataBox);
                    map.remove(index);
                }
            }
        }

        for (Entry<Integer, MetaValue> entry : map.entrySet()) {
            int index = entry.getKey();
            MetaValue v = entry.getValue();
            DataBox dataBox = new DataBox(v.getType(), v.getLocale(), v.getData());
            List<Box> children = new ArrayList<Box>();
            data.put(index, children);
            children.add(dataBox);
        }
        this.replaceBox(new IListBox(data));
    }

    private void dropChildBox(List<Box> children, String fourcc2) {
        ListIterator<Box> listIterator = children.listIterator();
        while (listIterator.hasNext()) {
            Box next = listIterator.next();
            if (fourcc2.equals(next.getFourcc())) {
                listIterator.remove();
            }
        }
    }

    public static String fourcc() {
        return FOURCC;
    }
}
