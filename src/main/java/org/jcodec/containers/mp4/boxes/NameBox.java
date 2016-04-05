package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.io.NIOUtils;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class NameBox extends Box {
    private String name;

    public static String fourcc() {
        return "name";
    }

    public static NameBox createNameBox(String name) {
        NameBox box = new NameBox(new Header(fourcc()));
        box.name = name;
        return box;
    }

    private NameBox(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        name = NIOUtils.readNullTermString(input);
    }

    protected void doWrite(ByteBuffer out) {
        out.putArr(JCodecUtil2.asciiString(name));
        out.putInt(0);
    }

    public String getName() {
        return name;
    }
}
