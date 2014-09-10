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
public class EbmlUint extends EbmlBin {

    public EbmlUint(byte[] id) {
        super(id);
    }
    
    public EbmlUint(byte[] id, long value) {
        super(id);
        set(value);
    }
    
    public void set(long value){
        this.data = ByteBuffer.wrap(longToBytes(value));
        this.dataLen = this.data.limit();
    }

    public long get() {
        long l = 0;
        long tmp = 0;
        for (int i = 0; i < data.limit(); i++) {
            tmp = ((long) data.get(data.limit() - 1 - i)) << 56;
            tmp >>>= (56 - (i * 8));
            l |= tmp;
        }
        return l;
    }
    
    public static byte[] longToBytes(long value) {
        byte[] b = new byte[calculatePayloadSize(value)];
        for (int i = b.length - 1; i >= 0; i--) {
            b[i] = (byte) (value >>> (8 * (b.length - i - 1)));
        }
        return b;
    }

    public static int calculatePayloadSize(long value) {
        if (value == 0)
            return 1;
        
        long mask = 0xFF00000000000000L;
        int i = 0;
        while ((value & (mask>>>8*i)) == 0 && i < 8)
            i++;
        
        return 8 - i;
    }

}
