package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Track fragment run
 * 
 * @author The JCodec project
 * 
 */
public class TrunBox extends FullBox {
    
    public static class Entry {
        int duration;
        int size;
        int flags;
        int cto;
    }

    public TrunBox() {
        super(new Header(fourcc()));
    }
    

    public static String fourcc() {
        return "stsc";
    }
    
    public void parse(ByteBuffer input) {
        
    }
    
    @Override
    public void doWrite(ByteBuffer out) {
        
    }
}
