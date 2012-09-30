package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;
import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Pixel aspect ratio video sample entry extension
 * 
 * @author The JCodec project
 *
 */
public class PixelAspectExt extends Box {
    private int hSpacing;
    private int vSpacing;

    public PixelAspectExt(Header header) {
        super(header);
    }

    public PixelAspectExt() {
        super(new Header(fourcc()));
    }

    public PixelAspectExt(Rational par) {
        this();
        this.hSpacing = par.getNum();
        this.vSpacing = par.getDen();
    }

    public void parse(InputStream input) throws IOException {
        hSpacing = (int) ReaderBE.readInt32(input);
        vSpacing = (int) ReaderBE.readInt32(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeInt(hSpacing);
        out.writeInt(vSpacing);
    }

    public int gethSpacing() {
        return hSpacing;
    }

    public int getvSpacing() {
        return vSpacing;
    }
    
    public Rational getRational() {
        return new Rational(hSpacing, vSpacing);
    }

    public static String fourcc() {
        return "pasp";
    }
}