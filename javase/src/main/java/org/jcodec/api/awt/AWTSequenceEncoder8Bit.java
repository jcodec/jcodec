package org.jcodec.api.awt;

import org.jcodec.api.SequenceEncoder8Bit;
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
public class AWTSequenceEncoder8Bit extends SequenceEncoder8Bit {

    public AWTSequenceEncoder8Bit(SeekableByteChannel out) throws IOException {
        super(out);
    }

    public static AWTSequenceEncoder8Bit createSequenceEncoder8Bit(File out) throws IOException {
        return new AWTSequenceEncoder8Bit(NIOUtils.writableChannel(out));
    }

    public void encodeImage(BufferedImage bi) throws IOException {
        encodeNativeFrame(AWTUtil.fromBufferedImageRGB8Bit(bi));
    }
}
