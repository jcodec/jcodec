package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.jcodec.common.io.NIOUtils;

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
    
    public UrlBox() {
        super(new Header(fourcc()));
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
        Charset utf8 = Charset.forName("utf-8");
        
        url = NIOUtils.readNullTermString(input, utf8);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        Charset utf8 = Charset.forName("utf-8");

        if (url != null) {
            NIOUtils.write(out, ByteBuffer.wrap(url.getBytes(utf8)));
            out.put((byte) 0);
        }
    }

    public String getUrl() {
        return url;
    }
}
