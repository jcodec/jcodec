package org.jcodec.containers.mp4.boxes;

import org.jcodec.containers.mp4.BoxUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 *
 */
public class DataInfoBox extends NodeBox {

    public static String fourcc() {
        return "dinf";
    }

    public static DataInfoBox createDataInfoBox() {
        return new DataInfoBox(new Header(fourcc()));
    }

    public DataInfoBox(Header atom) {
        super(atom);
    }

    public DataRefBox getDref() {
        return BoxUtil.findFirst(this, DataRefBox.class, "dref");
    }
}
