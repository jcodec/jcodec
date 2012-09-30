package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MediaBox extends NodeBox {

    public static String fourcc() {
        return "mdia";
    }

    public MediaBox(Header atom) {
        super(atom);
    }

    public MediaBox() {
        super(new Header(fourcc()));
    }

    public MediaInfoBox getMinf() {
        return Box.findFirst(this, MediaInfoBox.class, "minf");
    }
}
