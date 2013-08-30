package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

public class FielExtension extends Box {
    private int type;
    private int order;

    public FielExtension(byte type, byte order) {
        super(new Header(fourcc()));
        this.type = type;
        this.order = order;
    }

    public FielExtension() {
        super(new Header(fourcc()));
    }

    public static String fourcc() {
        return "fiel";
    }

    public boolean isInterlaced() {
        return type == 2;
    }
    
    public boolean topFieldFirst() {
        return order == 1 || order == 6;
    }

    public String getOrderInterpretation() {
        if (isInterlaced())
            // Copy from qtff 2007-09-04, page 98 The following defines
            // the permitted variants:
            // 0 There is only one field.
            switch (order) {
            case 1:
                // 1 T is displayed earliest, T is stored first in the file.
                return "top";
            case 6:
                // 6 B is displayed earliest, B is stored first in the file.
                return "bottom";
            case 9:
                // 9  B is displayed earliest, T is stored first in the file.
                return "bottomtop";
            case 14:
                // 14  T is displayed earliest, B is stored first in the
                // file.
                return "topbottom";
            }

        return "";
    }

    @Override
    public void parse(ByteBuffer input) {
        this.type = input.get() & 0xff;
        if (isInterlaced()) {
            this.order = input.get() & 0xff;
        }
    }

    @Override
    public void doWrite(ByteBuffer out) {
        out.put((byte) type);
        out.put((byte) order);
    }
}
