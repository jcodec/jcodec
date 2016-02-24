package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;

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
        out.put(JCodecUtil.asciiString(name));
        out.putInt(0);
    }

    public String getName() {
        return name;
    }
}
