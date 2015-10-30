package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MovieExtendsBox extends NodeBox {
    public static String fourcc() {
        return "mvex";
    }

    public MovieExtendsBox() {
        super(new Header(fourcc()));
    }
}
