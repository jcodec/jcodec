package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class UrlBox extends FullBox {

    private String url;

    public static String fourcc() {
        return "url ";
    }

    public UrlBox(String url) {
        super(new Header(fourcc()));
        this.url = url;
    }

    public UrlBox(Header atom) {
        super(atom);
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        if ((flags & 0x1) != 0)
            return;
        url = NIOUtils.readNullTermString(input);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        if (url != null) {
            NIOUtils.writePascalString(out, url);
        }
    }

    public String getUrl() {
        return url;
    }
}
