package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.model.Rational;

import java.nio.ByteBuffer;

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

    public static PixelAspectExt createPixelAspectExt(Rational par) {
        PixelAspectExt pasp = new PixelAspectExt(new Header(fourcc()));
        pasp.hSpacing = par.getNum();
        pasp.vSpacing = par.getDen();
        return pasp;
    }
    
    public void parse(ByteBuffer input) {
        hSpacing = input.getInt();
        vSpacing = input.getInt();
    }

    protected void doWrite(ByteBuffer out) {
        out.putInt(hSpacing);
        out.putInt(vSpacing);
    }
    
    @Override
    public int estimateSize() {
        return 16;
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