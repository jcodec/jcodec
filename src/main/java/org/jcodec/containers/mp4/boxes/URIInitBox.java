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
public class URIInitBox extends FullBox {

    public URIInitBox(Header atom) {
        super(atom);
    }

    private byte[] data;

    public static String fourcc() {
        return "uriI";
    }

    public static URIInitBox createURIInitBox(byte[] data) {
        URIInitBox uriInitBox = new URIInitBox(new Header(fourcc()));
        uriInitBox.data = data;
        return uriInitBox;
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        data = NIOUtils.toArray(input);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.put(data);
    }

    @Override
    public int estimateSize() {
        return 13 + data.length;
    }

    public byte[] getData() {
        return data;
    }
}
