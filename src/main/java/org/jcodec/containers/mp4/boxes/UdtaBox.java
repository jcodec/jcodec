package org.jcodec.containers.mp4.boxes;

import org.jcodec.containers.mp4.IBoxFactory;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class UdtaBox extends NodeBox {
    private static final String FOURCC = "udta";

    public UdtaBox() {
        this(Header.createHeader(fourcc(), 0));
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
}
