package org.jcodec.api.awt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jcodec.scale.AWTUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
@Deprecated
public class SequenceEncoder extends org.jcodec.api.SequenceEncoder {

    public SequenceEncoder(File out) throws IOException {
        super(out);
    }

    public void encodeImage(BufferedImage bi) throws IOException {
        encodeNativeFrame(AWTUtil.fromBufferedImage(bi));
    }
}
