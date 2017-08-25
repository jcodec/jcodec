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
     * Skips some number of frames in this source
     * 
     * @param skipFrames
     *            The number of frames to skip
     * @throws IOException
     */
    void seekFrames(int seekFrames, PixelStore pixelStore) throws IOException;

    /**
     * Gets the next video frame from this source
     * 
     * @return A picture holding the decoded frame, when the picture is not used it
     *         must be returned to the appropriate pixel store for the maximum
     *         efficiency.
     * @throws IOException
     */
    VideoFrameWithPacket getNextVideoFrame(PixelStore pixelStore) throws IOException;

    /**
     * Gets the next decoded audio frame from this source
     * 
     * @return The audio buffer containing PCM samples of the decoded audio and the
     *         audio format.
     */
    AudioFrameWithPacket getNextAudioFrame() throws IOException;

    /**
     * Closes the input and flushes all the buffers related to this source.
     */
    void finish();

    boolean haveAudio();

    void setOption(Options option, Object value);

    /**
     * Gets the metadata about video. In some cases full metadata may only be
     * available after the first video frame is decoded. Of all the metadata at
     * least the codec type will be available.
     * 
     * @return
     */
    VideoCodecMeta getVideoCodecMeta();
    
    /**
     * Same as above only returns an empty object if no video tracks exist in this
     * movie.
     * 
     * @return
     */
    VideoCodecMeta getVideoCodecMetaSafe();

    /**
     * Gets the metadata about audio.  In some cases full metadata may only be
     * available after the first audio frame is decoded. Of all the metadata at
     * least the codec type will be available.
     * 
     * @return
     */
    AudioCodecMeta getAudioCodecMeta();
    
    
    /**
     * Same as above only returns an empty object if no audio tracks exist in this
     * movie.
     * 
     * @return
     */
    AudioCodecMeta getAudioCodecMetaSafe();

    boolean isVideo();

    boolean isAudio();

    Format getFormat();
}