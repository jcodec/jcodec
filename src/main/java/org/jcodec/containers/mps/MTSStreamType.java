package org.jcodec.containers.mps;
import java.util.ArrayList;
import java.util.List;

public final class MTSStreamType {
    private final static List<MTSStreamType> _values = new ArrayList<MTSStreamType>();

    public final static MTSStreamType RESERVED = new MTSStreamType(0x0, false, false);
    public final static MTSStreamType VIDEO_MPEG1 = new MTSStreamType(0x01, true, false);
    public final static MTSStreamType VIDEO_MPEG2 = new MTSStreamType(0x02, true, false);
    public final static MTSStreamType AUDIO_MPEG1 = new MTSStreamType(0x03, false, true);
    public final static MTSStreamType AUDIO_MPEG2 = new MTSStreamType(0x04, false, true);
    public final static MTSStreamType PRIVATE_SECTION = new MTSStreamType(0x05, false, false);
    public final static MTSStreamType PRIVATE_DATA = new MTSStreamType(0x06, false, false);
    public final static MTSStreamType MHEG = new MTSStreamType(0x7, false, false);
    public final static MTSStreamType DSM_CC = new MTSStreamType(0x8, false, false);
    public final static MTSStreamType ATM_SYNC = new MTSStreamType(0x9, false, false);
    public final static MTSStreamType DSM_CC_A = new MTSStreamType(0xa, false, false);
    public final static MTSStreamType DSM_CC_B = new MTSStreamType(0xb, false, false);
    public final static MTSStreamType DSM_CC_C = new MTSStreamType(0xc, false, false);
    public final static MTSStreamType DSM_CC_D = new MTSStreamType(0xd, false, false);
    public final static MTSStreamType MPEG_AUX = new MTSStreamType(0xe, false, false);
    public final static MTSStreamType AUDIO_AAC_ADTS = new MTSStreamType(0x0f, false, true);
    public final static MTSStreamType VIDEO_MPEG4 = new MTSStreamType(0x10, true, false);
    public final static MTSStreamType AUDIO_AAC_LATM = new MTSStreamType(0x11, false, true);
    public final static MTSStreamType FLEXMUX_PES = new MTSStreamType(0x12, false, false);
    public final static MTSStreamType FLEXMUX_SEC = new MTSStreamType(0x13, false, false);
    public final static MTSStreamType DSM_CC_SDP = new MTSStreamType(0x14, false, false);
    public final static MTSStreamType META_PES = new MTSStreamType(0x15, false, false);
    public final static MTSStreamType META_SEC = new MTSStreamType(0x16, false, false);
    public final static MTSStreamType DSM_CC_DATA_CAROUSEL = new MTSStreamType(0x17, false, false);
    public final static MTSStreamType DSM_CC_OBJ_CAROUSEL = new MTSStreamType(0x18, false, false);
    public final static MTSStreamType DSM_CC_SDP1 = new MTSStreamType(0x19, false, false);
    public final static MTSStreamType IPMP = new MTSStreamType(0x1a, false, false);
    public final static MTSStreamType VIDEO_H264 = new MTSStreamType(0x1b, true, false);
    public final static MTSStreamType AUDIO_AAC_RAW = new MTSStreamType(0x1c, false, true);
    public final static MTSStreamType SUBS = new MTSStreamType(0x1d, false, false);
    public final static MTSStreamType AUX_3D = new MTSStreamType(0x1e, false, false);
    public final static MTSStreamType VIDEO_AVC_SVC = new MTSStreamType(0x1f, true, false);
    public final static MTSStreamType VIDEO_AVC_MVC = new MTSStreamType(0x20, true, false);
    public final static MTSStreamType VIDEO_J2K = new MTSStreamType(0x21, true, false);
    public final static MTSStreamType VIDEO_MPEG2_3D = new MTSStreamType(0x22, true, false);
    public final static MTSStreamType VIDEO_H264_3D = new MTSStreamType(0x23, true, false);
    public final static MTSStreamType VIDEO_CAVS = new MTSStreamType(0x42, false, true);
    public final static MTSStreamType IPMP_STREAM = new MTSStreamType(0x7f, false, false);
    public final static MTSStreamType AUDIO_AC3 = new MTSStreamType(0x81, false, true);
    public final static MTSStreamType AUDIO_DTS = new MTSStreamType(0x8a, false, true);

    private int tag;
    private boolean video;
    private boolean audio;

    private MTSStreamType(int tag, boolean video, boolean audio) {
        this.tag = tag;
        this.video = video;
        this.audio = audio;
        _values.add(this);
    }

    public static MTSStreamType[] values() {
        return _values.toArray(new MTSStreamType[0]);
    }

    public static MTSStreamType fromTag(int streamTypeTag) {
        MTSStreamType[] values = values();
        for (int i = 0; i < values.length; i++) {
            MTSStreamType streamType = values[i];
            if (streamType.tag == streamTypeTag)
                return streamType;
        }
        return null;
    }

    public int getTag() {
        return tag;
    }

    public boolean isVideo() {
        return video;
    }

    public boolean isAudio() {
        return audio;
    }
}