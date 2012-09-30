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
public class TimecodeMediaInfoBox extends FullBox {

    private short font;
    private short face;
    private short size;
    private short[] color = new short[3];
    private short[] bgcolor = new short[3];
    private String name;

    public static String fourcc() {
        return "tcmi";
    }

    public TimecodeMediaInfoBox(short font, short face, short size, short[] color, short[] bgcolor, String name) {
        this(new Header(fourcc()));
        this.font = font;
        this.face = face;
        this.size = size;
        this.color = color;
        this.bgcolor = bgcolor;
        this.name = name;
    }

    public TimecodeMediaInfoBox(Header atom) {
        super(atom);
    }

    @Override
    public void parse(InputStream input) throws IOException {
        super.parse(input);
        font = (short) ReaderBE.readInt16(input);
        face = (short) ReaderBE.readInt16(input);
        size = (short) ReaderBE.readInt16(input);
        ReaderBE.readInt16(input);
        color[0] = (short) ReaderBE.readInt16(input);
        color[1] = (short) ReaderBE.readInt16(input);
        color[2] = (short) ReaderBE.readInt16(input);
        bgcolor[0] = (short) ReaderBE.readInt16(input);
        bgcolor[1] = (short) ReaderBE.readInt16(input);
        bgcolor[2] = (short) ReaderBE.readInt16(input);
        name = ReaderBE.readPascalString(input);
    }

    @Override
    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeShort(font);
        out.writeShort(face);
        out.writeShort(size);
        out.writeShort(0);
        out.writeShort(color[0]);
        out.writeShort(color[1]);
        out.writeShort(color[2]);
        out.writeShort(bgcolor[0]);
        out.writeShort(bgcolor[1]);
        out.writeShort(bgcolor[2]);
        out.writeByte(name.length());
        out.write(name.getBytes());
    }
}
