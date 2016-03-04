package org.jcodec.common;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class VideoEncoder {
    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _out) {
        return encodeFrame8Bit(Picture8Bit.fromPicture(pic), _out);
    }

    public abstract ByteBuffer encodeFrame8Bit(Picture8Bit pic, ByteBuffer _out);

    public abstract ColorSpace[] getSupportedColorSpaces();
}