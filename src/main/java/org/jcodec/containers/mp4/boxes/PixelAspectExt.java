package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

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

    public void parse(ByteBuffer input) {
        hSpacing = input.getInt();
        vSpacing = input.getInt();
    }

    protected void doWrite(ByteBuffer out) {
        out.putInt(hSpacing);
        out.putInt(vSpacing);
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

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": " + hSpacing + ":" + vSpacing);
    }
}