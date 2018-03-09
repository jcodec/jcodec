package org.jcodec.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The list of formats known to JCodec
 * 
 * @author The JCodec project
 * 
 */
public final class Format {
    public final static Format MOV = new Format("MOV", true, true);
    public final static Format MPEG_PS = new Format("MPEG_PS", true, true);
    public final static Format MPEG_TS = new Format("MPEG_TS", true, true);
    public final static Format MKV = new Format("MKV", true, true);
    public final static Format H264 = new Format("H264", true, false);
    public final static Format RAW = new Format("RAW", true, true);
    public final static Format FLV = new Format("FLV", true, true);
    public final static Format AVI = new Format("AVI", true, true);
    public final static Format IMG = new Format("IMG", true, false);
    public final static Format IVF = new Format("IVF", true, false);
    public final static Format MJPEG = new Format("MJPEG", true, false);
    public final static Format Y4M = new Format("Y4M", true, false);
    public final static Format WAV = new Format("WAV", false, true);
    public final static Format WEBP = new Format("WEBP", true, false);
    public final static Format MPEG_AUDIO = new Format("MPEG_AUDIO", false, true);

    private final static Map<String, Format> _values = new LinkedHashMap<String, Format>();
    static {
        _values.put("MOV", MOV);
        _values.put("MPEG_PS", MPEG_PS);
        _values.put("MPEG_TS", MPEG_TS);
        _values.put("MKV", MKV);
        _values.put("H264", H264);
        _values.put("RAW", RAW);
        _values.put("FLV", FLV);
        _values.put("AVI", AVI);
        _values.put("IMG", IMG);
        _values.put("IVF", IVF);
        _values.put("MJPEG", MJPEG);
        _values.put("Y4M", Y4M);
        _values.put("WAV", WAV);
        _values.put("WEBP", WEBP);
        _values.put("MPEG_AUDIO", MPEG_AUDIO);
    }

    private final boolean video;
    private final boolean audio;
    private final String name;

    private Format(String name, boolean video, boolean audio) {
        this.name = name;
        this.video = video;
        this.audio = audio;
    }

    public boolean isAudio() {
        return audio;
    }

    public boolean isVideo() {
        return video;
    }

    public static Format valueOf(String s) {
        return _values.get(s);
    }
}
