package org.jcodec.common;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A generic muxer interface.
 * 
 * @author The JCodec project
 * 
 */
public interface Muxer {
    /**
     * Adds a video track and stores the provided metadata fields inside the
     * container. Note: some containers don't store all or any metadata fields
     * in which case some or all provided metadata will be ignored.
     * 
     * @param codec
     *            Codec type stored in this track.
     * @param meta
     *            Video metadata including the codec type.
     * @return A track used further to store media samples.
     */
    MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta);

    /**
     * Adds an audio track and stores the provided metadata fields inside the
     * container. Note: some containers don't store all or any metadata fields
     * in which case some or all provided metadata will be ignored.
     * 
     * @param codec
     *            Codec type stored in this track.
     * @param meta
     *            Audio metadata including the codec type.
     * @return A track used further to store media samples.
     */
    MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta);

    /**
     * Finalize writing this file. This function needs to be called at the end
     * of the muxing session since some muxers use global headers and those can
     * only be known at the end of the coding session. After calling this method
     * no further muxing is possible with this muxer though this check might not
     * be enforced by individual muxers. Calling any muxer methods after this
     * function returns yields an undefined behavior.
     * 
     * @throws IOException
     */
    void finish() throws IOException;
}
