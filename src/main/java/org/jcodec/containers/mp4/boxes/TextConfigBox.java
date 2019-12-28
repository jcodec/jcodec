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
public class TextConfigBox extends FullBox {

    private String textConfig;

    public TextConfigBox(Header atom) {
        super(atom);
    }

    public static String fourcc() {
        return "txtC";
    }

    public static TextConfigBox createTextConfigBox(String textConfig) {
        TextConfigBox box = new TextConfigBox(new Header(fourcc()));
        box.textConfig = textConfig;
        return box;
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        textConfig = NIOUtils.readNullTermStringCharset(input, Platform.UTF_8);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        NIOUtils.writeNullTermString(out, textConfig);
    }

    @Override
    public int estimateSize() {
        return 13 + Platform.getBytesForCharset(textConfig, Platform.UTF_8).length;
    }

    public String getTextConfig() {
        return textConfig;
    }
}
