package org.jcodec.containers.mp4.boxes;

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
public class GenericMediaInfoBox extends FullBox {
    private short graphicsMode;
    private short rOpColor;
    private short gOpColor;
    private short bOpColor;
    private short balance;

    public static String fourcc() {
        return "gmin";
    }

    public GenericMediaInfoBox(short graphicsMode, short rOpColor, short gOpColor, short bOpColor, short balance) {
        this();
        this.graphicsMode = graphicsMode;
        this.rOpColor = rOpColor;
        this.gOpColor = gOpColor;
        this.bOpColor = bOpColor;
        this.balance = balance;
    }

    public GenericMediaInfoBox(Header atom) {
        super(atom);
    }

    public GenericMediaInfoBox() {
        this(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        graphicsMode = (short) ReaderBE.readInt16(input);
        rOpColor = (short) ReaderBE.readInt16(input);
        gOpColor = (short) ReaderBE.readInt16(input);
        bOpColor = (short) ReaderBE.readInt16(input);
        balance = (short) ReaderBE.readInt16(input);
        ReaderBE.readInt16(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeShort(graphicsMode);
        out.writeShort(rOpColor);
        out.writeShort(gOpColor);
        out.writeShort(bOpColor);
        out.writeShort(balance);
        out.writeShort(0);
    }
}
