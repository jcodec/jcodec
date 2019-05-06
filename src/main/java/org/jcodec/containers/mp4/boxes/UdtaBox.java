package org.jcodec.containers.mp4.boxes;

import org.jcodec.containers.mp4.IBoxFactory;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public class UdtaBox extends NodeBox {
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

    public String latlng() {
        Box gps = findGps(this);
        if (gps == null) return null;
        ByteBuffer data = getData(gps);
        if (data == null) return null;
        if (data.remaining() < 4) return null;
        data.getInt(); //skip 4 bytes
        byte[] coordsBytes = new byte[data.remaining()];
        data.get(coordsBytes);
        String latlng = new String(coordsBytes);
        return latlng;
    }

    static Box findGps(UdtaBox udta) {
        List<Box> boxes1 = udta.getBoxes();
        for (Box box : boxes1) {
            if (box.getFourcc().endsWith("xyz")) {
                return box;
            }
        }
        return null;
    }

    static ByteBuffer getData(Box box) {
        if (box instanceof Box.LeafBox) {
            Box.LeafBox leaf = (Box.LeafBox) box;
            return leaf.getData();
        }
        return null;
    }


}
