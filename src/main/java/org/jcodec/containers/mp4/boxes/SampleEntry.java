package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class SampleEntry extends NodeBox {

    private short drefInd;

    public SampleEntry(Header header) {
        super(header);
    }

    public SampleEntry(Header header, short drefInd) {
        super(header);
        this.drefInd = drefInd;
    }

    public void parse(ByteBuffer input) {
        input.getInt();
        input.getShort();
        
        drefInd = input.getShort();
    }
    
    protected void parseExtensions(ByteBuffer input) {
        super.parse(input);
    }

    protected void doWrite(ByteBuffer out) {
        out.put(new byte[] { 0, 0, 0, 0, 0, 0 });
        out.putShort(drefInd); // data ref index
    }
    
    protected void writeExtensions(ByteBuffer out) {
        super.doWrite(out);
    }

    public short getDrefInd() {
        return drefInd;
    }

    public void setDrefInd(short ind) {
        this.drefInd = ind;
    }

    public void setMediaType(String mediaType) {
        header = new Header(mediaType);
    }
}
