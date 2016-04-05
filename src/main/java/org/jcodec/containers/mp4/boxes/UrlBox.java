package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.platform.Platform;

import js.nio.ByteBuffer;
import js.nio.charset.Charset;

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

    public static UrlBox createUrlBox(String url) {
        UrlBox urlBox = new UrlBox(new Header(fourcc()));
        urlBox.url = url;
        return urlBox;
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

        url = NIOUtils.readNullTermStringCharset(input, utf8);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        Charset utf8 = Charset.forName("utf-8");

        if (url != null) {
            NIOUtils.write(out, ByteBuffer.wrap(Platform.getBytesForCharset(url, utf8)));
            out.put((byte) 0);
        }
    }

    public String getUrl() {
        return url;
    }
}
