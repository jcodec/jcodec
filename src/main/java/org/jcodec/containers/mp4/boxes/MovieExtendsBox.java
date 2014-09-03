package org.jcodec.containers.mp4.boxes;

public class MovieExtendsBox extends NodeBox {
    public static String fourcc() {
        return "mvex";
    }

    public MovieExtendsBox() {
        super(new Header(fourcc()));
    }
}
