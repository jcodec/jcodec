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
public class ClearApertureBox extends FullBox {
    private float width;
    private float height;

    public static String fourcc() {
        return "clef";
    }

    public ClearApertureBox(Header atom) {
        super(atom);
    }

    public ClearApertureBox() {
        super(new Header(fourcc()));
    }

    public ClearApertureBox(int width, int height) {
        this();
        this.width = width;
        this.height = height;
    }

    public ClearApertureBox(Header header, int width, int height) {
        super(header);
        this.width = width;
        this.height = height;
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        width = ReaderBE.readInt32(input) / 65536f;
        height = ReaderBE.readInt32(input) / 65536f;
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt((int) (width * 65536f));
        out.writeInt((int) (height * 65536f));
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
