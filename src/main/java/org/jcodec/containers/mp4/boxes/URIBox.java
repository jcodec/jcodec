package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class URIBox extends FullBox {

    private String uri;
    
    public static String fourcc() {
        return "uri ";
    }

    public static URIBox createUriBox(String uri) {
        URIBox uriBox = new URIBox(new Header(fourcc()));
        uriBox.uri = uri;
        return uriBox;
    }

    public URIBox(Header atom) {
        super(atom);
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        uri = NIOUtils.readNullTermStringCharset(input, Platform.UTF_8);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        NIOUtils.writeNullTermString(out, uri);
    }

    @Override
    public int estimateSize() {
        return 13 + Platform.getBytesForCharset(uri, Platform.UTF_8).length;
    }

    public String getUri() {
        return uri;
    }
}
