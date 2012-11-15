package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public void parse(InputStream input) throws IOException {
        ReaderBE.readInt32(input);
        ReaderBE.readInt16(input);
        
        drefInd = (short) ReaderBE.readInt16(input);
    }
    
    protected void parseExtensions(InputStream input) throws IOException {
        super.parse(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(new byte[] { 0, 0, 0, 0, 0, 0 });
        out.writeShort(drefInd); // data ref index
    }
    
    protected void writeExtensions(DataOutput out) throws IOException {
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
