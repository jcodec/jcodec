package org.jcodec.api.transcode;

import java.io.IOException;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Format;
import org.jcodec.common.VideoCodecMeta;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public interface Source {

    /**
     * Starts reading media data from the source.
     * 
     * @param pixelStore
     *            The pixel store where the buffers for the returned pictures
     *            will be borrowed from.
     * 
     * @throws IOException
     */
    void init(PixelStore pixelStore) throws IOException;

    /**
     * Skips some number of frames in this source
     * 
     * @param skipFrames
     *            The number of frames to skip
     * @throws IOException
     */
    void seekFrames(int seekFrames) throws IOException;

    /**
     * Gets the next video frame from this source
     * 
     * @return A picture holding the decoded frame, when the picture is not used
     *         it must be returned to the appropriate pixel store for the
     *         maximum efficiency.
     * @throws IOException
     */
    VideoFrameWithPacket getNextVideoFrame() throws IOException;

    /**
     * Gets the next decoded audio frame from this source
     * 
     * @return The audio buffer containing PCM samples of the decoded audio and
     *         the audio format.
     */
    AudioFrameWithPacket getNextAudioFrame() throws IOException;

    /**
     * Closes the input and flushes all the buffers related to this source.
     */
    void finish();

    boolean haveAudio();

    void setOption(Options option, Object value);

    /**
     * Gets the metadata about video
     * @return
     */
    VideoCodecMeta getVideoCodecMeta();
    
    /**
     * Gets the metadata about audio
     * @return
     */
    AudioCodecMeta getAudioCodecMeta();

    Format getInputFormat();
}