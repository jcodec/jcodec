package org.jcodec.common;

import static org.jcodec.common.TrackType.AUDIO;
import static org.jcodec.common.TrackType.VIDEO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum Codec {
    MPEG2(VIDEO), PRORES(VIDEO), H264(VIDEO), J2K(VIDEO), MPEG4(VIDEO), DV(VIDEO), VC1(
            VIDEO), VC3(VIDEO), V210(VIDEO), ALAW(AUDIO), AC3(AUDIO), MP3(AUDIO), MP2(AUDIO), MP1(AUDIO), DTS(
            AUDIO), PCM_DVD(AUDIO), TRUEHD(AUDIO), PCM(AUDIO), ADPCM(AUDIO), NELLYMOSER(
            AUDIO), G711(AUDIO), AAC(AUDIO), SPEEX(AUDIO), SORENSON(VIDEO), FLASH_SCREEN_VIDEO(
            VIDEO), VP6(VIDEO), FLASH_SCREEN_V2(VIDEO), PNG(VIDEO), VP8(VIDEO), VP9(VIDEO), JPEG(
            VIDEO), RAW(null), VORBIS(VIDEO), TIMECODE(TrackType.OTHER);

    private TrackType type;

    private Codec(TrackType type) {
        this.type = type;
    }

    public TrackType getType() {
        return type;
    }

    public static Codec codecByFourcc(String fourcc) {
        if (fourcc.equals("avc1")) {
            return H264;
        } else if (fourcc.equals("m1v1") || fourcc.equals("m2v1")) {
            return MPEG2;
        } else if (fourcc.equals("apco") || fourcc.equals("apcs") || fourcc.equals("apcn") || fourcc.equals("apch")
                || fourcc.equals("ap4h")) {
            return PRORES;
        } else if (fourcc.equals("mp4a")) {
            return AAC;
        } else if (fourcc.equals("jpeg")) {
            return JPEG;
        }
        return null;
    }
}