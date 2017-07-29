package org.jcodec.common;
import java.nio.ByteBuffer;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class VideoEncoder {
    public static class EncodedFrame {
        private ByteBuffer data;
        private boolean keyFrame;
        
        public EncodedFrame(ByteBuffer data, boolean keyFrame) {
            this.data = data;
            this.keyFrame = keyFrame;
        }
        
        public ByteBuffer getData() {
            return data;
        }
        public boolean isKeyFrame() {
            return keyFrame;
        }
    }
    
    public abstract EncodedFrame encodeFrame(Picture pic, ByteBuffer _out);

    public abstract ColorSpace[] getSupportedColorSpaces();

    /**
     * Estimate the output buffer size that will likely be needed for the
     * current instance of encoder to encode a given frame. Note: expect a very
     * coarse estimate that reflects the settings the encoder has been created
     * with as well as the input frame size.
     * 
     * @param frame
     *            A frame in question.
     * @return The number of bytes the encoded frame will likely take.
     */
    public abstract int estimateBufferSize(Picture frame);
}