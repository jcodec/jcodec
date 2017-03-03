package org.jcodec.api.transcode;

import static org.jcodec.common.Tuple._2;
import static org.jcodec.common.Tuple._3;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.TrackType;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.api.transcode.Transcoder.Filter;
import org.jcodec.api.transcode.filters.DumpMvFilter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transcoder command line.
 * 
 * @author The JCodec project
 * 
 */
public class TranscodeMain {
    private static final String FLAG_SEEK_FRAMES = "seek-frames";
    private static final String FLAG_MAX_FRAMES = "max-frames";

    private static final String FLAG_OUTPUT_AUDIO_CODEC = "o:ac";
    private static final String FLAG_INPUT_AUDIO_CODEC = "i:ac";
    private static final String FLAG_OUTPUT_VIDEO_CODEC = "o:vc";
    private static final String FLAG_INPUT_VIDEO_CODEC = "i:vc";
    private static final String FLAG_OUTPUT_FORMAT = "o:f";
    private static final String FLAG_INPUT_FORMAT = "i:f";

    private static final String FLAG_PROFILE = "profile";
    private static final String FLAG_INTERLACED = "interlaced";

    private static final String FLAG_DUMPMV = "dumpMv";
    private static final String FLAG_DUMPMVJS = "dumpMvJs";

    private static final String FLAG_DOWNSCALE = "downscale";

    private static Map<String, Format> extensionToF = new HashMap<String, Format>();
    private static Map<String, Codec> extensionToC = new HashMap<String, Codec>();
    private static Map<Format, Codec> videoCodecsForF = new HashMap<Format, Codec>();
    private static Map<Format, Codec> audioCodecsForF = new HashMap<Format, Codec>();
    private static Set<Codec> supportedDecoders = new HashSet<Codec>();

    static {
        extensionToF.put("mpg", Format.MPEG_PS);
        extensionToF.put("mpeg", Format.MPEG_PS);
        extensionToF.put("m2p", Format.MPEG_PS);
        extensionToF.put("ps", Format.MPEG_PS);
        extensionToF.put("vob", Format.MPEG_PS);
        extensionToF.put("evo", Format.MPEG_PS);
        extensionToF.put("mod", Format.MPEG_PS);
        extensionToF.put("tod", Format.MPEG_PS);

        extensionToF.put("ts", Format.MPEG_TS);
        extensionToF.put("m2t", Format.MPEG_TS);

        extensionToF.put("mp4", Format.MOV);
        extensionToF.put("m4a", Format.MOV);
        extensionToF.put("m4v", Format.MOV);
        extensionToF.put("mov", Format.MOV);
        extensionToF.put("3gp", Format.MOV);

        extensionToF.put("mkv", Format.MKV);
        extensionToF.put("webm", Format.MKV);

        extensionToF.put("264", Format.H264);
        extensionToF.put("jsv", Format.H264);
        extensionToF.put("h264", Format.H264);
        extensionToF.put("raw", Format.RAW);
        extensionToF.put("", Format.RAW);
        extensionToF.put("flv", Format.FLV);
        extensionToF.put("avi", Format.AVI);
        extensionToF.put("jpg", Format.IMG);
        extensionToF.put("jpeg", Format.IMG);
        extensionToF.put("png", Format.IMG);

        extensionToF.put("mjp", Format.MJPEG);

        extensionToF.put("ivf", Format.IVF);
        extensionToF.put("y4m", Format.Y4M);
        extensionToF.put("wav", Format.WAV);

        extensionToC.put("mpg", Codec.MPEG2);
        extensionToC.put("mpeg", Codec.MPEG2);
        extensionToC.put("m2p", Codec.MPEG2);
        extensionToC.put("ps", Codec.MPEG2);
        extensionToC.put("vob", Codec.MPEG2);
        extensionToC.put("evo", Codec.MPEG2);
        extensionToC.put("mod", Codec.MPEG2);
        extensionToC.put("tod", Codec.MPEG2);
        extensionToC.put("ts", Codec.MPEG2);
        extensionToC.put("m2t", Codec.MPEG2);
        extensionToC.put("m4a", Codec.AAC);
        extensionToC.put("mkv", Codec.H264);
        extensionToC.put("webm", Codec.VP8);
        extensionToC.put("264", Codec.H264);
        extensionToC.put("raw", Codec.RAW);
        extensionToC.put("jpg", Codec.JPEG);
        extensionToC.put("jpeg", Codec.JPEG);
        extensionToC.put("png", Codec.PNG);
        extensionToC.put("mjp", Codec.JPEG);
        extensionToC.put("y4m", Codec.RAW);

        videoCodecsForF.put(Format.MPEG_PS, Codec.MPEG2);
        audioCodecsForF.put(Format.MPEG_PS, Codec.MP2);
        videoCodecsForF.put(Format.MOV, Codec.H264);
        audioCodecsForF.put(Format.MOV, Codec.AAC);
        videoCodecsForF.put(Format.MKV, Codec.VP8);
        audioCodecsForF.put(Format.MKV, Codec.VORBIS);
        audioCodecsForF.put(Format.WAV, Codec.PCM);
        videoCodecsForF.put(Format.H264, Codec.H264);
        videoCodecsForF.put(Format.RAW, Codec.RAW);
        videoCodecsForF.put(Format.FLV, Codec.H264);
        videoCodecsForF.put(Format.AVI, Codec.MPEG4);
        videoCodecsForF.put(Format.IMG, Codec.PNG);
        videoCodecsForF.put(Format.MJPEG, Codec.JPEG);
        videoCodecsForF.put(Format.IVF, Codec.VP8);
        videoCodecsForF.put(Format.Y4M, Codec.RAW);

        supportedDecoders.add(Codec.AAC);
        supportedDecoders.add(Codec.H264);
        supportedDecoders.add(Codec.JPEG);
        supportedDecoders.add(Codec.MPEG2);
        supportedDecoders.add(Codec.PCM);
        supportedDecoders.add(Codec.PNG);
        supportedDecoders.add(Codec.PRORES);
        supportedDecoders.add(Codec.RAW);
        supportedDecoders.add(Codec.VP8);
    }

    public static void main(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args);
        if (args.length < 2) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {

                    put(FLAG_INPUT_FORMAT, "Input format [default=auto].");
                    put(FLAG_OUTPUT_FORMAT, "Output format [default=auto].");
                    put(FLAG_INPUT_VIDEO_CODEC, "Input video codec [default=auto].");
                    put(FLAG_OUTPUT_VIDEO_CODEC, "Output video codec [default=auto].");
                    put(FLAG_INPUT_AUDIO_CODEC, "Input audio codec [default=auto].");
                    put(FLAG_OUTPUT_AUDIO_CODEC, "Output audio codec [default=auto].");
                    put(FLAG_SEEK_FRAMES, "Seek frames");
                    put(FLAG_MAX_FRAMES, "Max frames");
                    put(FLAG_PROFILE, "Profile to use (supported by some encoders).");
                    put(FLAG_INTERLACED, "Encode output as interlaced (supported by Prores encoder).");
                    put(FLAG_DUMPMV, "Dump motion vectors (supported by h.264 decoder).");
                    put(FLAG_DUMPMVJS, "Dump motion vectors in form of JASON file (supported by h.264 decoder).");
                    put(FLAG_DOWNSCALE, "Decode frames in downscale (supported by MPEG, Prores and Jpeg decoders).");
                }
            }, "input", "output");
            return;
        }

        String input = cmd.getArg(0);
        String output = cmd.getArg(1);

        String inputFormatRaw = cmd.getStringFlag(FLAG_INPUT_FORMAT);
        Format inputFormat;
        if (inputFormatRaw == null) {
            inputFormat = getFormatFromExtension(input);
            if (inputFormat != Format.IMG) {
                Format detectFormat = JCodecUtil.detectFormat(new File(input));
                if (detectFormat != null)
                    inputFormat = detectFormat;
            }
        } else {
            inputFormat = Format.valueOf(inputFormatRaw.toUpperCase());
        }
        if (inputFormat == null) {
            Logger.error("Input format could not be detected");
            return;
        }

        String outputFormatRaw = cmd.getStringFlag(FLAG_OUTPUT_FORMAT);
        Format outputFormat;
        if (outputFormatRaw == null) {
            outputFormat = getFormatFromExtension(output);
        } else {
            outputFormat = Format.valueOf(outputFormatRaw.toUpperCase());
        }

        int videoTrackNo = -1;
        String inputCodecVideoRaw = cmd.getStringFlag(FLAG_INPUT_VIDEO_CODEC);
        _3<Integer, Integer, Codec> inputCodecVideo = null;
        if (inputCodecVideoRaw == null) {
            if (inputFormat == Format.IMG) {
                inputCodecVideo = _3(0, 0, getCodecFromExtension(input));
            } else if (inputFormat.isVideo()) {
                inputCodecVideo = selectSuitableTrack(input, inputFormat, TrackType.VIDEO);
            }
        } else {
            inputCodecVideo = _3(0, 0, Codec.valueOf(inputCodecVideoRaw.toUpperCase()));
        }

        String outputCodecVideoRaw = cmd.getStringFlag(FLAG_OUTPUT_VIDEO_CODEC);
        Codec outputCodecVideo = null;
        boolean videoCopy = false;
        if (outputCodecVideoRaw == null) {
            outputCodecVideo = getCodecFromExtension(output);
            if (outputCodecVideo == null)
                outputCodecVideo = getFirstVideoCodecForFormat(outputFormat);
        } else {
            if ("copy".equalsIgnoreCase(outputCodecVideoRaw)) {
                videoCopy = true;
                outputCodecVideo = inputCodecVideo.v2;
            } else if("none".equalsIgnoreCase(outputCodecVideoRaw)) {
                outputCodecVideo = null;
                inputCodecVideo = null;
            } else {
                outputCodecVideo = Codec.valueOf(outputCodecVideoRaw.toUpperCase());
            }
        }

        String inputCodecAudioRaw = cmd.getStringFlag(FLAG_INPUT_AUDIO_CODEC);
        _3<Integer, Integer, Codec> inputCodecAudio = null;
        if (inputCodecAudioRaw == null) {
            if (inputFormat.isAudio()) {
                inputCodecAudio = selectSuitableTrack(input, inputFormat, TrackType.AUDIO);
            }
        } else {
            inputCodecAudio = _3(0, 0, Codec.valueOf(inputCodecAudioRaw.toUpperCase()));
        }

        String outputCodecAudioRaw = cmd.getStringFlag(FLAG_OUTPUT_AUDIO_CODEC);
        Codec outputCodecAudio = null;
        boolean audioCopy = false;
        if (outputCodecAudioRaw == null) {
            if (outputFormat.isAudio())
                outputCodecAudio = getFirstAudioCodecForFormat(outputFormat);
        } else {
            if ("copy".equalsIgnoreCase(outputCodecAudioRaw)) {
                audioCopy = true;
                outputCodecAudio = inputCodecAudio.v2;
            } else if("none".equalsIgnoreCase(outputCodecVideoRaw)) {
                outputCodecAudio = null;
                inputCodecAudio = null;
            } else {
                outputCodecAudio = Codec.valueOf(outputCodecAudioRaw.toUpperCase());
            }
        }
        if (inputCodecAudio == null)
            outputCodecAudio = null;

        List<Filter> filters = new ArrayList<Filter>();
        if (cmd.getBooleanFlag(FLAG_DUMPMV))
            filters.add(new DumpMvFilter(false));
        else if (cmd.getBooleanFlag(FLAG_DUMPMVJS))
            filters.add(new DumpMvFilter(true));

        Transcoder transcoder = new Transcoder(cmd.getArg(0), cmd.getArg(1), inputFormat, outputFormat, inputCodecVideo,
                outputCodecVideo, inputCodecAudio, outputCodecAudio, videoCopy, audioCopy, filters);
        transcoder.setSeekFrames(cmd.getIntegerFlagD(FLAG_SEEK_FRAMES, 0));
        transcoder.setMaxFrames(cmd.getIntegerFlagD(FLAG_MAX_FRAMES, Integer.MAX_VALUE));
        transcoder.setProfile(cmd.getStringFlag(FLAG_PROFILE));
        transcoder.setInterlaced(cmd.getBooleanFlagD(FLAG_INTERLACED, false));
        Integer downscale = cmd.getIntegerFlagD(FLAG_DOWNSCALE, 1);
        if (downscale != null && (1 << MathUtil.log2(downscale)) != downscale) {
            Logger.error(
                    "Only values [2, 4, 8] are supported for " + FLAG_DOWNSCALE + ", the option will have no effect.");
        } else {
            transcoder.setDownscale(downscale);
        }
        transcoder.transcode();
    }

    private static Codec getFirstAudioCodecForFormat(Format inputFormat) {
        return audioCodecsForF.get(inputFormat);
    }

    private static Codec getFirstVideoCodecForFormat(Format inputFormat) {
        return videoCodecsForF.get(inputFormat);
    }

    private static Codec detectVideoDecoder(DemuxerTrack track) throws IOException {
        DemuxerTrackMeta meta = track.getMeta();
        if (meta != null) {
            Codec codec = meta.getCodec();
            if (codec != null)
                return codec;
        }
        Packet packet = track.nextFrame();
        if (packet == null)
            return null;

        return JCodecUtil.detectDecoder(packet.getData());
    }

    private static _3<Integer, Integer, Codec> selectSuitableTrack(String input, Format format, TrackType targetType)
            throws IOException {
        _2<Integer, Demuxer> demuxerPid;
        if (format == Format.MPEG_TS) {
            demuxerPid = JCodecUtil.createM2TSDemuxer(new File(input), targetType);
        } else {
            demuxerPid = _2(0, JCodecUtil.createDemuxer(format, new File(input)));
        }
        if(demuxerPid == null || demuxerPid.v1 == null)
            return null;
        int trackNo = 0;
        List<? extends DemuxerTrack> tracks = targetType == TrackType.VIDEO ? demuxerPid.v1.getVideoTracks()
                : demuxerPid.v1.getAudioTracks();
        for (DemuxerTrack demuxerTrack : tracks) {
            Codec codec = detectVideoDecoder(demuxerTrack);
            if (supportedDecoders.contains(codec)) {
                return _3(demuxerPid.v0, trackNo, codec);
            }
            trackNo++;
        }
        return null;
    }

    private static Format getFormatFromExtension(String output) {
        String extension = output.replaceFirst(".*\\.([^\\.]+$)", "$1");
        return extensionToF.get(extension);
    }

    private static Codec getCodecFromExtension(String output) {
        String extension = output.replaceFirst(".*\\.([^\\.]+$)", "$1");
        return extensionToC.get(extension);
    }

    public static Set<Format> formats(Format... formats) {
        return new HashSet<Format>(Arrays.asList(formats));
    }

    public static Set<Codec> codecs(Codec... codecs) {
        return new HashSet<Codec>(Arrays.asList(codecs));
    }
}