package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class MediaInfoBox extends NodeBox {

    public static String fourcc() {
        return "minf";
    }

    public static MediaInfoBox createMediaInfoBox() {
        return new MediaInfoBox(new Header(fourcc()));
    }

    public MediaInfoBox(Header atom) {
        super(atom);
    }

    public DataInfoBox getDinf() {
        return findFirst(this, DataInfoBox.class, "dinf");
    }

    public NodeBox getStbl() {
        return findFirst(this, NodeBox.class, "stbl");
    }
}
