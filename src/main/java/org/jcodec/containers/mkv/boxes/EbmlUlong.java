package org.jcodec.containers.mkv.boxes;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlUlong extends EbmlBin {


    public EbmlUlong(byte[] id) {
        super(id);
        data = ByteBuffer.allocate(8);
    }
    
    public static EbmlUlong createEbmlUlong(byte[] id, long value) {
        EbmlUlong e = new EbmlUlong(id);
        e.setUlong(value);
        return e;
    }
    
    public void setUlong(long value){
        data.putLong(value);
        data.flip();
    }

    public long getUlong() {
        return data.duplicate().getLong();
    }

}
