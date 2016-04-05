package org.jcodec.containers.mp4.boxes;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * 
 * @author The JCodec project
 * 
 */
public class GamaExtension extends Box {

    private float gamma;

    public static GamaExtension createGamaExtension(float gamma) {
        GamaExtension gamaExtension = new GamaExtension(new Header(fourcc()));
        gamaExtension.gamma = gamma;
        return gamaExtension;
    }

    public GamaExtension(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        float g = input.getInt();
        gamma = g / 65536f;
    }

    protected void doWrite(ByteBuffer out) {
        out.putInt((int) (gamma * 65536));
    }

    public float getGamma() {
        return gamma;
    }

    public static String fourcc() {
        return "gama";
    }

}