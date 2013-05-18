package org.jcodec.player;

import org.jcodec.codecs.pcm.PCMDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.AudioDecoder;
import org.jcodec.player.filters.MediaInfo;

public class PlayerUtils {
    public static AudioDecoder getAudioDecoder(String fourcc, MediaInfo.AudioInfo info) {
        if ("sowt".equals(fourcc) || "in24".equals(fourcc) || "twos".equals(fourcc) || "in32".equals(fourcc))
            return new PCMDecoder(info);
        else if ("s302".equals(fourcc))
            return new S302MDecoder();
        else
            return null;
    }
}
