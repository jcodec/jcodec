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
public class MdtaBox extends Box {

    private static final String FOURCC = "mdta";
    private String key;

    public MdtaBox(Header header) {
        super(header);
    }

    public MdtaBox(String key) {
        this(Header.createHeader(FOURCC, 0));
        this.key = key;
    }

    @Override
    public void parse(ByteBuffer buf) {
        key = new String(NIOUtils.toArray(NIOUtils.readBuf(buf)));
    }

    public String getKey() {
        return key;
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        out.put(key.getBytes());
    }

    @Override
    public int estimateSize() {
        return key.getBytes().length;
    }
    
    public static String fourcc() {
        return FOURCC;
    }
}
