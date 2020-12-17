package org.jcodec.common;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.jcodec.common.TrackType.AUDIO;
import static org.jcodec.common.TrackType.VIDEO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 *
 */
public final class Codec {
    public final static Codec H265 = new Codec("H265", VIDEO, false);
    public final static Codec H264 = new Codec("H264", VIDEO, false);
    public final static Codec MPEG2 = new Codec("MPEG2", VIDEO, false);
    public final static Codec MPEG4 = new Codec("MPEG4", VIDEO, false);
    public final static Codec PRORES = new Codec("PRORES", VIDEO, false);
    public final static Codec DV = new Codec("DV", VIDEO, false);
    public final static Codec VC1 = new Codec("VC1", VIDEO, false);
    public final static Codec VC3 = new Codec("VC3", VIDEO, false);
    public final static Codec V210 = new Codec("V210", VIDEO, false);
    public final static Codec SORENSON = new Codec("SORENSON", VIDEO, false);
    public final static Codec FLASH_SCREEN_VIDEO = new Codec("FLASH_SCREEN_VIDEO", VIDEO, false);
    public final static Codec FLASH_SCREEN_V2 = new Codec("FLASH_SCREEN_V2", VIDEO, false);
    public final static Codec PNG = new Codec("PNG", VIDEO, false);
    public final static Codec JPEG = new Codec("JPEG", VIDEO, false);
    public final static Codec J2K = new Codec("J2K", VIDEO, false);
    public final static Codec VP6 = new Codec("VP6", VIDEO, false);
    public final static Codec VP8 = new Codec("VP8", VIDEO, false);
    public final static Codec VP9 = new Codec("VP9", VIDEO, false);
    public final static Codec VORBIS = new Codec("VORBIS", AUDIO, false);
    public final static Codec AAC = new Codec("AAC", AUDIO, false);
    public final static Codec MP3 = new Codec("MP3", AUDIO, false);
    public final static Codec MP2 = new Codec("MP2", AUDIO, false);
    public final static Codec MP1 = new Codec("MP1", AUDIO, false);
    public final static Codec AC3 = new Codec("AC3", AUDIO, false);
    public final static Codec DTS = new Codec("DTS", AUDIO, false);
    public final static Codec TRUEHD = new Codec("TRUEHD", AUDIO, false);
    public final static Codec PCM_DVD = new Codec("PCM_DVD", AUDIO, true);
    public final static Codec PCM = new Codec("PCM", AUDIO, true);
    public final static Codec ADPCM = new Codec("ADPCM", AUDIO, false);
    public final static Codec ALAW = new Codec("ALAW", AUDIO, true);
    public final static Codec NELLYMOSER = new Codec("NELLYMOSER", AUDIO, false);
    public final static Codec G711 = new Codec("G711", AUDIO, false);
    public final static Codec SPEEX = new Codec("SPEEX", AUDIO, false);
    public final static Codec OPUS = new Codec("OPUS", AUDIO, false);
    public final static Codec RAW = new Codec("RAW", null, false);
    public final static Codec TIMECODE = new Codec("TIMECODE", TrackType.OTHER, false);

    private final static Map<String, Codec> _values = new LinkedHashMap<String, Codec>();
    static {
        _values.put("H264", H264);
        _values.put("MPEG2", MPEG2);
        _values.put("MPEG4", MPEG4);
        _values.put("PRORES", PRORES);
        _values.put("DV", DV);
        _values.put("VC1", VC1);
        _values.put("VC3", VC3);
        _values.put("V210", V210);
        _values.put("SORENSON", SORENSON);
        _values.put("FLASH_SCREEN_VIDEO", FLASH_SCREEN_VIDEO);
        _values.put("FLASH_SCREEN_V2", FLASH_SCREEN_V2);
        _values.put("PNG", PNG);
        _values.put("JPEG", JPEG);
        _values.put("J2K", J2K);
        _values.put("VP6", VP6);
        _values.put("VP8", VP8);
        _values.put("VP9", VP9);
        _values.put("VORBIS", VORBIS);
        _values.put("AAC", AAC);
        _values.put("MP3", MP3);
        _values.put("MP2", MP2);
        _values.put("MP1", MP1);
        _values.put("AC3", AC3);
        _values.put("DTS", DTS);
        _values.put("TRUEHD", TRUEHD);
        _values.put("PCM_DVD", PCM_DVD);
        _values.put("PCM", PCM);
        _values.put("ADPCM", ADPCM);
        _values.put("ALAW", ALAW);
        _values.put("NELLYMOSER", NELLYMOSER);
        _values.put("G711", G711);
        _values.put("SPEEX", SPEEX);
        _values.put("RAW", RAW);
        _values.put("OPUS", OPUS);
        _values.put("TIMECODE", TIMECODE);
    }

    private final String _name;
    private final TrackType type;
    private boolean pcm;

    public Codec(String name, TrackType type, boolean pcm) {
        this._name = name;
        this.type = type;
        this.pcm = pcm;
    }

    public TrackType getType() {
        return type;
    }

    public static Codec codecByFourcc(String fourcc) {
        if (fourcc.equals("hev1")) {
            return H265;
        } else if (fourcc.equals("avc1")) {
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

    public static Codec valueOf(String s) {
        return _values.get(s);
    }

    @Override
    public String toString() {
        return _name;
    }

    public String name() {
        return _name;
    }

    public boolean isPcm() {
        return pcm;
    }
}