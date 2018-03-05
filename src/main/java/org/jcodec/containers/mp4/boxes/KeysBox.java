package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.Boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class KeysBox extends NodeBox {
    private static final String FOURCC = "keys";

    private static class LocalBoxes extends Boxes {
        //Initializing blocks are not supported by Javascript.
        LocalBoxes() {
            super();
            mappings.put(MdtaBox.fourcc(), MdtaBox.class);
        }
    }

    public KeysBox(Header atom) {
        super(atom);
        factory = new SimpleBoxFactory(new LocalBoxes());
    }

    public static KeysBox createKeysBox() {
        return new KeysBox(Header.createHeader(FOURCC, 0));
    }

    public void parse(ByteBuffer input) {
        int vf = input.getInt();
        int cnt = input.getInt();
        super.parse(input);
    }

    protected void doWrite(ByteBuffer out) {
        out.putInt(0);
        out.putInt(boxes.size());
        super.doWrite(out);
    }

    public static String fourcc() {
        return FOURCC;
    }
    
    @Override
    public int estimateSize() {
        return 8 + super.estimateSize();
    }
}
