package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EncodedPixelBox extends ClearApertureBox {

    public static String fourcc() {
        return "enof";
    }

    public static EncodedPixelBox createEncodedPixelBox(int width, int height) {
        EncodedPixelBox enof = new EncodedPixelBox(new Header(fourcc()));
        enof.width = width;
        enof.height = height;
        return enof;
    }

    public EncodedPixelBox(Header atom) {
        super(atom);
    }
}