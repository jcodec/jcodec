package org.jcodec.player.filters.http;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.MediaInfo.AudioInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Downloads frames and media info from JCodec streaming server
 * 
 * @author The JCodec project
 * 
 */
public class MediaInfoParser {

    public static MediaInfo[] parseMediaInfos(String encoded) {
        JSONArray tracks = (JSONArray) JSONValue.parse(encoded);
        MediaInfo[] infos = new MediaInfo[tracks.size()];
        int i = 0;
        for (Object track : tracks) {
            MediaInfo mi = parseMediaInfo((JSONObject) track);
            if (mi != null)
                infos[i++] = mi;
        }
        return infos;
    }

    public static MediaInfo parseMediaInfo(String encoded) {
        return parseMediaInfo((JSONObject) JSONValue.parse(encoded));
    }

    private static MediaInfo parseMediaInfo(JSONObject track) {
        String type = (String) track.get("type");
        if ("video".equals(type)) {
            return parseVideo((JSONObject) track.get("info"));
        } else if ("audio".equals(type)) {
            return parseAudio((JSONObject) track.get("info"));
        }
        return null;
    }

    private static MediaInfo parseAudio(JSONObject info) {
        MediaInfo gen = parseMedia(info);
        JSONObject transcodedInfo = (JSONObject) info.get("transcodedFrom");
        if (transcodedInfo != null)
            gen.setTranscodedFrom(parseAudio(transcodedInfo));

        return new AudioInfo(gen, parseFormat((JSONObject) info.get("af")),
                ((Number) info.get("framesPerPacket")).intValue(), parseLabels((JSONArray) info.get("labels")));
    }

    private static ChannelLabel[] parseLabels(JSONArray labels) {
        ChannelLabel[] result = new ChannelLabel[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            result[i] = ChannelLabel.valueOf((String) labels.get(i));
        }
        return result;
    }

    private static AudioFormat parseFormat(JSONObject format) {
        return new AudioFormat(((Number) format.get("sampleRate")).intValue(),
                ((Number) format.get("sampleSizeInBits")).intValue(), ((Number) format.get("channels")).intValue(),
                true, (Boolean) format.get("bigEndian"));
    }

    private static MediaInfo parseVideo(JSONObject info) {
        MediaInfo gen = parseMedia(info);
        JSONObject transcodedInfo = (JSONObject) info.get("transcodedFrom");
        if (transcodedInfo != null)
            gen.setTranscodedFrom(parseVideo(transcodedInfo));

        return new MediaInfo.VideoInfo(gen, parseRational((JSONObject) info.get("par")),
                parseSize((JSONObject) info.get("dim")));
    }

    private static Rational parseRational(JSONObject rational) {
        return new Rational(((Number) rational.get("num")).intValue(), ((Number) rational.get("den")).intValue());
    }

    private static Size parseSize(JSONObject size) {
        return new Size(((Number) size.get("width")).intValue(), ((Number) size.get("height")).intValue());
    }

    private static MediaInfo parseMedia(JSONObject info) {
        return new MediaInfo((String) info.get("fourcc"), ((Number) info.get("timescale")).intValue(),
                ((Number) info.get("duration")).intValue(), ((Number) info.get("nFrames")).intValue(),
                (String) info.get("name"), null);
    }
}
