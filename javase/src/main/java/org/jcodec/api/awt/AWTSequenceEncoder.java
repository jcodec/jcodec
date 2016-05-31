package org.jcodec.api.awt;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
@Deprecated
public class AWTSequenceEncoder extends SequenceEncoder {

    public static AWTSequenceEncoder createSequenceEncoder(File out) throws IOException {
        return new AWTSequenceEncoder(NIOUtils.writableChannel(out));
    }

    public AWTSequenceEncoder(SeekableByteChannel ch) throws IOException {
        super(ch);
    }

    public void encodeImage(BufferedImage bi) throws IOException {
        encodeNativeFrame(AWTUtil.fromBufferedImageRGB(bi));
    }
}
