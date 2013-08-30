package org.jcodec.common;

import java.nio.ByteBuffer;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface VideoEncoder {
    ByteBuffer encodeFrame(Picture pic, ByteBuffer _out);
    
    ColorSpace[] getSupportedColorSpaces();
}
