package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Movie fragment header box
 * 
 * @author The JCodec project
 * 
 */
public class MovieExtendsHeaderBox extends FullBox {
    public MovieExtendsHeaderBox(Header atom) {
        super(atom);
    }

    private int fragmentDuration;

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
