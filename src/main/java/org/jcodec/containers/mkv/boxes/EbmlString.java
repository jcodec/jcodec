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
public class EbmlString extends EbmlBin {
    
    public String charset = "UTF-8";

    public EbmlString(byte[] id) {
        super(id);
    }
    
    public static EbmlString createEbmlString(byte[] id, String value) {
        EbmlString e = new EbmlString(id);
        e.setString(value);
        return e;
    }
    
    public String getString() {
        try {
            return new String(data.array(), charset);
        } catch (java.io.UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return "";
        }
    }
    
    public void setString(String value){
        try {
            this.data = ByteBuffer.wrap(value.getBytes(charset));
        } catch (java.io.UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

}
