package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DataBox extends Box {
    private static final String FOURCC = "data";
    private int type;
    private int locale;
    private byte[] data;

    public DataBox(Header header) {
        super(header);
    }
    
    public DataBox(int type, int locale, byte[] data) {
        this(Header.createHeader(FOURCC, 0));
        this.type = type;
        this.locale = locale;
        this.data = data;
    }

    @Override
    public void parse(ByteBuffer buf) {
        type = buf.getInt();
        locale = buf.getInt();
        data = NIOUtils.toArray(NIOUtils.readBuf(buf));
    }

    public int getType() {
        return type;
    }

    public int getLocale() {
        return locale;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        out.putInt(type);
        out.putInt(locale);
        out.put(data);
    }

    public static String fourcc() {
        return FOURCC;
    }
}
