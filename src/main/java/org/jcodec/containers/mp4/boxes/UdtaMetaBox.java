package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The meata box inside 'udta'
 * 
 * @author The JCodec project
 * 
 */
public class UdtaMetaBox extends MetaBox {
    public UdtaMetaBox(Header atom) {
        super(atom);
    }

    public static UdtaMetaBox createUdtaMetaBox() {
        return new UdtaMetaBox(Header.createHeader(fourcc(), 0));
    }
    
    @Override
    public void parse(ByteBuffer input) {
        input.getInt();
        super.parse(input);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        out.putInt(0);
        super.doWrite(out);
    }
}
