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
public class EbmlString extends EbmlBin {
    
    public String charset = "UTF-8";

    public EbmlString(byte[] id) {
        super(id);
    }
    
    public EbmlString(byte[] id, String value){
        super(id);
        set(value);
    }
    
    public String get() {
        try {
            return new String(data.array(), charset);
        } catch (java.io.UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return "";
        }
    }
    
    public void set(String value){
        try {
            this.data = ByteBuffer.wrap(value.getBytes(charset));
        } catch (java.io.UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

}
