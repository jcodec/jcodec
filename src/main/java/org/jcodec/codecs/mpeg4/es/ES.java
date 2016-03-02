package org.jcodec.codecs.mpeg4.es;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ES extends NodeDescriptor {
    private int trackId;
    
    public ES(int trackId, Descriptor[] children) {
        super(tag(), children);
        this.trackId = trackId;
    }

    public static int tag() {
        return 0x03;
    }

    protected void doWrite(ByteBuffer out) {
        out.putShort((short)trackId);
        out.put((byte)0);
        super.doWrite(out);
    }
    
    protected void parse(ByteBuffer input) {
        trackId = input.getShort();
        input.get();
        super.parse(input);
    }

    public int getTrackId() {
        return trackId;
    }
}
