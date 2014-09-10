package org.jcodec.containers.mkv.boxes;

import java.nio.ByteBuffer;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author Jay Codec
 * 
 */
public class EbmlFloat extends EbmlBin {

    public EbmlFloat(byte[] id) {
        super(id);
    }
    
    public void set(double value) {
        if (value < Float.MAX_VALUE) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putFloat((float) value);
            bb.flip();
            this.data = bb;

        } else if (value < Double.MAX_VALUE) {
            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.putDouble(value);
            bb.flip();
            this.data = bb;

        }
    }

    public double get() {

        if (data.limit() == 4)
            return data.duplicate().getFloat();
        

        return data.duplicate().getDouble();

    }

}
