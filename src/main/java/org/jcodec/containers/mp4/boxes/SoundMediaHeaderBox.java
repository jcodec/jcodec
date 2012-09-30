package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Sound media header
 * 
 * @author The JCodec project
 * 
 */
public class SoundMediaHeaderBox extends FullBox {
    private short balance;
    
    public static String fourcc() {
        return "smhd";
    }

    public SoundMediaHeaderBox(Header atom) {
        super(atom);
    }
    
    public SoundMediaHeaderBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        balance = (short)ReaderBE.readInt16(input);
        ReaderBE.readInt16(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeShort(balance);
        out.writeShort(0);
    }

    public short getBalance() {
        return balance;
    }
}
