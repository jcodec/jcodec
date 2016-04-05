package org.jcodec.containers.mp4.boxes;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sound media header
 * 
 * @author The JCodec project
 * 
 */
public class SoundMediaHeaderBox extends FullBox {
    private short balance;

    public static String fourcc() {
        return "smhd";
    }

    public static SoundMediaHeaderBox createSoundMediaHeaderBox() {
        return new SoundMediaHeaderBox(new Header(fourcc()));
    }

    public SoundMediaHeaderBox(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        balance = input.getShort();
        input.getShort();
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putShort(balance);
        out.putShort((short) 0);
    }

    public short getBalance() {
        return balance;
    }
}
