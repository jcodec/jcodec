package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public NameBox(String name) {
        this();
        this.name = name;
    }

    public NameBox() {
        super(new Header(fourcc()));
    }

    private NameBox(Header header) {
        super(header);
    }

    public void parse(InputStream input) throws IOException {
        name = ReaderBE.readNullTermString(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(name.getBytes());
        out.writeInt(0);
    }

    public String getName() {
        return name;
    }
}
