package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The list of formats known to JCodec
 * 
 * @author The JCodec project
 * 
 */
public enum Format {
    MOV(true, true), MPEG_PS(true, true), MPEG_TS(true, true), MKV(true, true), H264(true, false), RAW(true, true), FLV(
            true, true), AVI(true, true), IMG(true, false), IVF(true, false), MJPEG(true, false), Y4M(true, false), WAV(
            false, true), WEBP(true, false), MPEG_AUDIO(false, true);

    private boolean video;
    private boolean audio;

    private Format(boolean video, boolean audio) {
        this.video = video;
        this.audio = audio;
    }

    public boolean isAudio() {
        return audio;
    }

    public boolean isVideo() {
        return video;
    }
}
