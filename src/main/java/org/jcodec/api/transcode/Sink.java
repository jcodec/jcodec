package org.jcodec.api.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public interface Sink {

    /**
     * Initializes output to this sink.
     * 
     * @throws IOException
     */
    void init(boolean videoCopy, boolean audioCopy) throws IOException;

    /**
     * Outputs video frame to the sink
     * 
     * @param decodedFrame
     * @throws IOException
     */
    void outputVideoFrame(VideoFrameWithPacket videoFrame) throws IOException;

    /**
     * Outputs an audio frame to the sink
     * 
     * @param audioFrame
     * @throws IOException
     */
    void outputAudioFrame(AudioFrameWithPacket audioFrame) throws IOException;

    /**
     * Finilizes encoding process, flushes the buffers and closes off the
     * output file (or any other resources for that matter).
     * 
     * @throws IOException
     */
    void finish() throws IOException;
    
    /**
     * Gets the color space that the sink expects
     * @return
     */
    ColorSpace getInputColor();

    void setOption(Options profile, Object value);

    boolean isVideo();
    boolean isAudio();

    void setVideoCodecPrivate(ByteBuffer videoCodecPrivate);

    void setAudioCodecPrivate(ByteBuffer audioCodecPrivate);

    void setCodecOpts(Map<String, String> codecOpts);
}