package org.jcodec.api.transcode;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.api.transcode.Transcoder.TranscoderBuilder;
import org.jcodec.api.transcode.filters.DumpMvFilter;
import org.jcodec.api.transcode.filters.ScaleFilter;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.common.tools.MainUtils.FlagType;
import org.jcodec.common.tools.MathUtil;

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
    private static final Flag FLAG_INPUT = new Flag("input", "i", "Designates an input argument", FlagType.VOID);
    private static final Flag FLAG_MAP_VIDEO = new Flag("map:v", "mv",
            "Map a video from a specified input into this output");
    private static final Flag FLAG_MAP_AUDIO = new Flag("map:a", "ma",
            "Map a audio from a specified input into this output");
    private static final Flag FLAG_SEEK_FRAMES = new Flag("seek-frames", "Seek frames");
    private static final Flag FLAG_MAX_FRAMES = new Flag("max-frames", "limit", "Max frames");

    private static final Flag FLAG_AUDIO_CODEC = new Flag("codec:audio", "acodec", "Audio codec [default=auto].");
    private static final Flag FLAG_VIDEO_CODEC = new Flag("codec:video", "vcodec", "Video codec [default=auto].");
    private static final Flag FLAG_FORMAT = new Flag("format", "f", "Format [default=auto].");

    private static final Flag FLAG_PROFILE = new Flag("profile", "Profile to use (supported by some encoders).");
    private static final Flag FLAG_INTERLACED = new Flag("interlaced",
            "Encode output as interlaced (supported by Prores encoder).");

    private static final Flag FLAG_DUMPMV = new Flag("dumpMv", "Dump motion vectors (supported by h.264 decoder).");
    private static final Flag FLAG_DUMPMVJS = new Flag("dumpMvJs",
            "Dump motion vectors in form of JASON file (supported by h.264 decoder).");

    private static final Flag FLAG_DOWNSCALE = new Flag("downscale",
            "Decode frames in downscale (supported by MPEG, Prores and Jpeg decoders).");
    
    private static final Flag FLAG_VIDEO_FILTER = new Flag("videoFilter", "vf",
            "Contains a comma separated list of video filters with arguments.");

    private static final Flag[] ALL_FLAGS = new Flag[] { FLAG_INPUT, FLAG_FORMAT, FLAG_VIDEO_CODEC, FLAG_AUDIO_CODEC,
            FLAG_SEEK_FRAMES, FLAG_MAX_FRAMES, FLAG_PROFILE, FLAG_INTERLACED, FLAG_DUMPMV, FLAG_DUMPMVJS,
            FLAG_DOWNSCALE, FLAG_MAP_VIDEO, FLAG_MAP_AUDIO, FLAG_VIDEO_FILTER };

    private static Map<String, Format> extensionToF = new HashMap<String, Format>();
    private static Map<String, Codec> extensionToC = new HashMap<String, Codec>();
    private static Map<Format, Codec> videoCodecsForF = new HashMap<Format, Codec>();
    private static Map<Format, Codec> audioCodecsForF = new HashMap<Format, Codec>();
    private static Map<String, Class<? extends Filter>> knownFilters = new HashMap<String, Class<? extends Filter>>();

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

        knownFilters.put("scale", ScaleFilter.class);
    }

    public static void main(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args, ALL_FLAGS);

        TranscoderBuilder builder = Transcoder.newTranscoder();

        List<Source> sources = new ArrayList<Source>();
        // All the inputs and related flags
        for (int index = 0; index < cmd.argsLength(); index++) {
            if (!cmd.getBooleanFlagI(index, FLAG_INPUT))
                continue;
            String input = cmd.getArg(index);

            Source source = SourceImpl.create(input);
            Integer downscale = cmd.getIntegerFlagID(index, FLAG_DOWNSCALE, 1);
            if (downscale != null && (1 << MathUtil.log2(downscale)) != downscale) {
                Logger.error("Only values [2, 4, 8] are supported for " + FLAG_DOWNSCALE
                        + ", the option will have no effect.");
            } else {
                source.setOption(Options.DOWNSCALE, downscale);
            }
            source.setOption(Options.PROFILE, cmd.getStringFlagI(index, FLAG_PROFILE));
            source.setOption(Options.INTERLACED, cmd.getBooleanFlagID(index, FLAG_INTERLACED, false));
            sources.add(source);
            builder.addSource(source);
            builder.setSeekFrames(sources.size() - 1, cmd.getIntegerFlagID(index, FLAG_SEEK_FRAMES, 0))
                    .setMaxFrames(sources.size() - 1, cmd.getIntegerFlagID(index, FLAG_MAX_FRAMES, Integer.MAX_VALUE));
        }

        if (sources.isEmpty()) {
            MainUtils.printHelpVarArgs(ALL_FLAGS, "input", "output");
            return;
        }

        // All the outputs and related flags
        List<Sink> sinks = new ArrayList<Sink>();
        for (int index = 0; index < cmd.argsLength(); index++) {
            if (cmd.getBooleanFlagI(index, FLAG_INPUT))
                continue;
            String output = cmd.getArg(index);
            String outputFormatRaw = cmd.getStringFlagI(index, FLAG_FORMAT);
            Format outputFormat;
            if (outputFormatRaw == null) {
                outputFormat = getFormatFromExtension(output);
            } else {
                outputFormat = Format.valueOf(outputFormatRaw.toUpperCase());
            }
            String outputCodecVideoRaw = cmd.getStringFlagI(index, FLAG_VIDEO_CODEC);
            Codec outputCodecVideo = null;
            boolean videoCopy = false;
            if (outputCodecVideoRaw == null) {
                outputCodecVideo = getCodecFromExtension(output);
                if (outputCodecVideo == null)
                    outputCodecVideo = getFirstVideoCodecForFormat(outputFormat);
            } else {
                if ("copy".equalsIgnoreCase(outputCodecVideoRaw)) {
                    videoCopy = true;
                } else if ("none".equalsIgnoreCase(outputCodecVideoRaw)) {
                    outputCodecVideo = null;
                } else {
                    outputCodecVideo = Codec.valueOf(outputCodecVideoRaw.toUpperCase());
                }
            }

            String outputCodecAudioRaw = cmd.getStringFlagI(index, FLAG_AUDIO_CODEC);
            Codec outputCodecAudio = null;
            boolean audioCopy = false;
            if (outputCodecAudioRaw == null) {
                if (outputFormat.isAudio())
                    outputCodecAudio = getFirstAudioCodecForFormat(outputFormat);
            } else {
                if ("copy".equalsIgnoreCase(outputCodecAudioRaw)) {
                    audioCopy = true;
                } else if ("none".equalsIgnoreCase(outputCodecVideoRaw)) {
                    outputCodecAudio = null;
                } else {
                    outputCodecAudio = Codec.valueOf(outputCodecAudioRaw.toUpperCase());
                }
            }

            int audioMap = cmd.getIntegerFlagID(index, FLAG_MAP_AUDIO, 0);
            if (audioMap > sources.size()) {
                Logger.error(
                        "Can not map audio from source " + audioMap + ", " + sources.size() + " sources specified.");
            }
            int videoMap = cmd.getIntegerFlagID(index, FLAG_MAP_VIDEO, 0);
            if (videoMap > sources.size()) {
                Logger.error(
                        "Can not map video from source " + videoMap + ", " + sources.size() + " sources specified.");
            }
            if (videoCopy) {
                Codec inputCodecVideo = sources.get(videoMap).getVideoCodecMeta().getCodec();
                outputCodecVideo = inputCodecVideo != null ? inputCodecVideo : null;
            }
            if (audioCopy) {
                Codec inputCodecAudio = sources.get(audioMap).getAudioCodecMeta().getCodec();
                outputCodecAudio = inputCodecAudio != null ? inputCodecAudio : null;
            }

            Sink sink = new SinkImpl(output, outputFormat, outputCodecVideo, outputCodecAudio);
            sinks.add(sink);
            builder.addSink(sink);
            builder.setAudioMapping(audioMap, sinks.size() - 1, audioCopy);
            builder.setVideoMapping(videoMap, sinks.size() - 1, videoCopy);

            // Custom filters
            if (cmd.getBooleanFlagI(index, FLAG_DUMPMV))
                builder.addFilter(sinks.size() - 1, new DumpMvFilter(false));
            else if (cmd.getBooleanFlagI(index, FLAG_DUMPMVJS))
                builder.addFilter(sinks.size() - 1, new DumpMvFilter(true));
            
            String vf = cmd.getStringFlagI(index, FLAG_VIDEO_FILTER);
            if (vf != null) {
                addVideoFilters(vf, builder, sinks.size() - 1);
            }
        }

        if (sources.isEmpty() || sinks.isEmpty()) {
            MainUtils.printHelpVarArgs(ALL_FLAGS, "input", "output");
            return;
        }

        Transcoder transcoder = builder.create();

        transcoder.transcode();
    }

    private static void addVideoFilters(String vf, TranscoderBuilder builder, int sinkIndex) {
        if (vf == null)
            return;
        for (String filter : vf.split(",")) {
            String[] parts = filter.split("=");
            String filterName = parts[0];
            Class<? extends Filter> filterClass = knownFilters.get(filterName);
            if (filterClass == null) {
                Logger.error("Unknown filter: " + filterName);
                throw new RuntimeException("Unknown filter: " + filterName);
            }
            if (parts.length > 1) {
                String filterArgs = parts[1];
                String[] split = filterArgs.split(":");
                Integer[] params = new Integer[split.length];
                Class[] types = new Class[split.length];
                for (int i = 0; i < split.length; i++) {
                    params[i] = Integer.parseInt(split[i]);
                    types[i] = int.class;
                }
                try {
                    Constructor<? extends Filter> constructor = filterClass.getConstructor(types);
                    Filter f = constructor.newInstance(params);
                    builder.addFilter(sinkIndex, f);
                } catch (Exception e) {
                    String message = "The filter " + filterName + " doesn't take " + split.length + " arguments.";
                    Logger.error(message);
                    throw new RuntimeException(message);
                }
            }
        }
    }

    private static Codec getFirstAudioCodecForFormat(Format inputFormat) {
        return audioCodecsForF.get(inputFormat);
    }

    private static Codec getFirstVideoCodecForFormat(Format inputFormat) {
        return videoCodecsForF.get(inputFormat);
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