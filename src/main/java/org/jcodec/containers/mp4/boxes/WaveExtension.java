package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Wave extension to audio sample entry
 * 
 * @author The JCodec project
 * 
 */
public class WaveExtension extends NodeBox {
    public static String fourcc() {
        return "wave";
    }

    public WaveExtension(Header atom) {
        super(atom);

    }
}