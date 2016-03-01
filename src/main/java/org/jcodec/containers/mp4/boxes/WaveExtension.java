package org.jcodec.containers.mp4.boxes;

import org.jcodec.codecs.mpeg4.mp4.EsdsBox;

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