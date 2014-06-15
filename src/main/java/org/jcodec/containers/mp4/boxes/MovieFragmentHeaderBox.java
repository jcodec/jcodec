package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * Movie fragment header box
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MovieFragmentHeaderBox extends FullBox {

    private int sequenceNumber;

    public MovieFragmentHeaderBox() {
        super(new Header(fourcc()));
    }

    public static String fourcc() {
        return "mfhd";
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        sequenceNumber = input.getInt();
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(sequenceNumber);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}
