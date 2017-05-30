package org.jcodec.common;

public class AudioCodecMeta {

    private AudioFormat format;

    public AudioCodecMeta(AudioFormat format) {
        this.format = format;
    }

    public AudioFormat getFormat() {
        return format;
    }
}
