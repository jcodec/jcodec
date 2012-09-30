package org.jcodec.codecs.mpeg4.es;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ES extends NodeDescriptor {
    private int trackId;
    
    public ES(int tag, int size) {
        super(tag, size);
    }

    public ES(int trackId, Descriptor... children) {
        super(tag(), children);
        this.trackId = trackId;
    }

    public static int tag() {
        return 0x03;
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeShort(trackId);
        out.write(0);
        super.doWrite(out);
    }
    
    protected void parse(InputStream input) throws IOException {
        trackId = (int)ReaderBE.readInt16(input);
        input.read();
        super.parse(input);
    }

    public int getTrackId() {
        return trackId;
    }
}
