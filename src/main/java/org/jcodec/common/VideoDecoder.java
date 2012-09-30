package org.jcodec.common;

import java.io.IOException;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface VideoDecoder {
    /**
     * Decodes a video frame to an uncompressed picture in codec native
     * colorspace
     * 
     * @param data
     *            Compressed frame data
     * @throws IOException
     */
    Picture decodeFrame(Buffer data, int[][] buffer);

    /**
     * Tests if compressed frame can be decoded with this decoder
     * 
     * @param data
     *            Compressed frame data
     * @return
     */
    int probe(Buffer data);
}