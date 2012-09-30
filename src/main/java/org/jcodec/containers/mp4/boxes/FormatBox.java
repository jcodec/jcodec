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
public class FormatBox extends Box {
    private String fmt;

    public FormatBox(Box other) {
        super(other);
    }

    public FormatBox(Header header) {
        super(header);
    }

    public FormatBox(String fmt) {
        super(new Header(fourcc()));
        this.fmt = fmt;
    }

    public static String fourcc() {
        return "frma";
    }

    public void parse(InputStream input) throws IOException {
        this.fmt = ReaderBE.readString(input, 4);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(fmt.getBytes());
    }
}