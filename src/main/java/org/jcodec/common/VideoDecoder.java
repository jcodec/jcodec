package org.jcodec.common;

import org.jcodec.common.model.Picture;

import js.io.IOException;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
abstract public class VideoDecoder {
    private byte[][] byteBuffer;
    
    /**
     * Decodes a video frame to an uncompressed picture in codec native
     * colorspace
     * 
     * @param data
     *            Compressed frame data
     * @throws IOException
     */
    public abstract Picture decodeFrame(ByteBuffer data, byte[][] buffer);
    public abstract VideoCodecMeta getCodecMeta(ByteBuffer data);

   
    protected byte[][] getSameSizeBuffer(int[][] buffer) {
        if (byteBuffer == null || byteBuffer.length != buffer.length || byteBuffer[0].length != buffer[0].length)
            byteBuffer = ArrayUtil.create2D(buffer[0].length, buffer.length);
        return byteBuffer;
    }
    
    /**
     * Returns a downscaled version of this decoder
     * @param ratio
     * @return
     */
    public VideoDecoder downscaled(int ratio) {
        if(ratio == 1)
            return this;
        return null;
    }
}