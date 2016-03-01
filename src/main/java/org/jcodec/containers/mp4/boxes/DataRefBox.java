package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class DataRefBox extends NodeBox {

    public static String fourcc() {
        return "dref";
    }

    public static DataRefBox createDataRefBox() {
        return new DataRefBox(new Header(fourcc()));
    }

    public DataRefBox(Header atom) {
        super(atom);
    }

    @Override
    public void parse(ByteBuffer input) {
        input.getInt();
        input.getInt();
        super.parse(input);
    }

    @Override
    public void doWrite(ByteBuffer out) {
        out.putInt(0);
        out.putInt(boxes.size());
        super.doWrite(out);
    }
}
