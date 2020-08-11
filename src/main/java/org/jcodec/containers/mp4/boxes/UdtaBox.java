package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.IBoxFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public class UdtaBox extends NodeBox {
    private static final String META_GPS = "©xyz";
    private static final int LOCALE_EN_US = 0x15c7;
    private static Map<String, Integer> knownMetadata = new HashMap<String, Integer>();
    static {
        knownMetadata.put(META_GPS, MetaValue.TYPE_STRING_UTF8);
    }

    private static final String FOURCC = "udta";

    public static UdtaBox createUdtaBox() {
        return new UdtaBox(Header.createHeader(fourcc(), 0));
    }

    @Override
    public void setFactory(final IBoxFactory _factory) {
        factory = new IBoxFactory() {
            @Override
            public Box newBox(Header header) {
                if (header.getFourcc().equals(UdtaMetaBox.fourcc())) {
                    UdtaMetaBox box = new UdtaMetaBox(header);
                    box.setFactory(_factory);
                    return box;
                }

                return _factory.newBox(header);
            }
        };
    }

    public UdtaBox(Header atom) {
        super(atom);
    }

    public MetaBox meta() {
        return NodeBox.findFirst(this, MetaBox.class, MetaBox.fourcc());
    }

    public static String fourcc() {
        return FOURCC;
    }

    public List<MetaValue> getUserDataString(String id) {
        LeafBox leafBox = NodeBox.findFirst(this, LeafBox.class, id);
        if (leafBox == null)
            return null;
        ByteBuffer bb = leafBox.getData();
        return parseStringData(bb);
    }

    private List<MetaValue> parseStringData(ByteBuffer bb) {
        List<MetaValue> result = new ArrayList<MetaValue>();
        while (bb.remaining() >= 4) {
            int lang = 0;
            int sz = NIOUtils.duplicate(bb).getInt();
            if (4 + sz > bb.remaining()) {
                sz = bb.getShort();
                if (2 + sz > bb.remaining())
                    break;
                lang = bb.getShort();
            }
            if (sz != 0) {
                byte[] bytes = new byte[sz];
                bb.get(bytes);
                result.add(MetaValue.createStringWithLocale(new String(bytes), lang));
            }
        }
        return result;
    }

    public ByteBuffer serializeStringData(List<MetaValue> data) {
        int totalLen = 0;
        for (MetaValue mv : data) {
            String string = mv.getString();
            if (string != null)
                totalLen += string.getBytes().length + 4;
        }
        if (totalLen == 0)
            return null;
        byte[] bytes = new byte[totalLen];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for (MetaValue mv : data) {
            String string = mv.getString();
            if (string != null) {
                bb.putShort((short) string.length());
                // en_US
                bb.putShort((short) mv.getLocale());
                bb.put(string.getBytes());
            }
        }
        ((java.nio.Buffer)bb).flip();
        return bb;
    }

    public void setUserDataString(String id, List<MetaValue> data) {
        ByteBuffer bb = serializeStringData(data);
        LeafBox box = LeafBox.createLeafBox(Header.createHeader(id, bb.remaining() + 4), bb);
        replaceBox(box);
    }

    public String latlng() {
        List<MetaValue> data = getUserDataString(META_GPS);
        if (data == null || data.isEmpty())
            return null;
        return data.get(0).getString();
    }

    public void setLatlng(String value) {
        List<MetaValue> list = new ArrayList<MetaValue>();
        // en_US
        list.add(MetaValue.createStringWithLocale(value, LOCALE_EN_US));
        setUserDataString(META_GPS, list);
    }

    public Map<Integer, MetaValue> getMetadata() {
        Map<Integer, MetaValue> result = new HashMap<Integer, MetaValue>();
        List<Box> boxes = getBoxes();
        for (Box box : boxes) {
            if (knownMetadata.containsKey(box.getFourcc()) && (box instanceof LeafBox)) {
                ByteBuffer data = ((LeafBox) box).getData();
                int type = knownMetadata.get(box.getFourcc());
                // only string for now :(
                if (type != MetaValue.TYPE_STRING_UTF8)
                    continue;
                List<MetaValue> value = parseStringData(data);
                byte[] bytes;
                try {
                    bytes = box.getFourcc().getBytes("iso8859-1");
                } catch (UnsupportedEncodingException e) {
                    bytes = null;
                }
                if (bytes != null && value != null && !value.isEmpty())
                    result.put(ByteBuffer.wrap(bytes).getInt(), value.get(0));
            }
        }

        return result;
    }

    public void setMetadata(Map<Integer, MetaValue> udata) {
        Set<Entry<Integer, MetaValue>> entrySet = udata.entrySet();
        for (Entry<Integer, MetaValue> entry : entrySet) {
            List<MetaValue> lst = new LinkedList<MetaValue>();
            lst.add(entry.getValue());
            ByteBuffer bb = serializeStringData(lst);
            byte[] bytes = new byte[4];
            ByteBuffer.wrap(bytes).putInt(entry.getKey());
            LeafBox box;
            try {
                box = LeafBox.createLeafBox(Header.createHeader(new String(bytes, "iso8859-1"), bb.remaining() + 4),
                        bb);
                replaceBox(box);
            } catch (UnsupportedEncodingException e) {
            }
        }
    }
}
