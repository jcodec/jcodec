package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum Codec {
    MPEG2(TrackType.VIDEO), PRORES(TrackType.VIDEO), H264(TrackType.VIDEO), J2K(TrackType.VIDEO), MPEG4(TrackType.VIDEO), DV(TrackType.VIDEO), VC3(
            TrackType.VIDEO), V210(TrackType.VIDEO), ALAW(TrackType.AUDIO), AC3(TrackType.AUDIO), MP3(TrackType.AUDIO), MP2(TrackType.AUDIO), DTS(
            TrackType.AUDIO), PCM_DVD(TrackType.AUDIO), TRUEHD(TrackType.AUDIO), PCM(TrackType.AUDIO), ADPCM(TrackType.AUDIO), NELLYMOSER(
            TrackType.AUDIO), G711(TrackType.AUDIO), AAC(TrackType.AUDIO), SPEEX(TrackType.AUDIO), SORENSON(TrackType.VIDEO), FLASH_SCREEN_VIDEO(
            TrackType.VIDEO), VP6(TrackType.VIDEO), FLASH_SCREEN_V2(TrackType.VIDEO), PNG(TrackType.VIDEO), VP8(TrackType.VIDEO), VP9(TrackType.VIDEO), JPEG(
            TrackType.VIDEO), RAW(null), VORBIS(TrackType.VIDEO), TIMECODE(TrackType.OTHER);

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