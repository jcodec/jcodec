package org.jcodec.containers.mkv.boxes;

import java.nio.ByteBuffer;

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
    
    public EbmlUlong(byte[] id, long value) {
        super(id);
        set(value);
    }
    
    public void set(long value){
        data.putLong(value);
        data.flip();
    }

    public long get() {
        return data.duplicate().getLong();
    }

}
