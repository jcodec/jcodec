package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * Movie fragment header box
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MovieExtendsHeaderBox extends FullBox {

    private int fragmentDuration;

    public MovieExtendsHeaderBox() {
        super(new Header(fourcc()));
    }

    public static String fourcc() {
        return "mehd";
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        fragmentDuration = input.getInt();
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(fragmentDuration);
    }

    public int getFragmentDuration() {
        return fragmentDuration;
    }

    public void setFragmentDuration(int fragmentDuration) {
        this.fragmentDuration = fragmentDuration;
    }
}
