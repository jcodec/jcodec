package org.jcodec.samples.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Basic transcoder interface.
 * 
 * @author The JCodec project
 * 
 */
public class TranscodeMain {

    private static final String FLAG_OUTPUT_AUDIO_CODEC = "o:ac";
    private static final String FLAG_INPUT_AUDIO_CODEC = "i:ac";
    private static final String FLAG_OUTPUT_VIDEO_CODEC = "o:vc";
    private static final String FLAG_INPUT_VIDEO_CODEC = "i:vc";
    private static final String FLAG_OUTPUT_FORMAT = "o:f";
    private static final String FLAG_INPUT_FORMAT = "i:f";

    private static List<Profile> profiles = new ArrayList<Profile>();
    private static Map<String, Format> extensionToF = new HashMap<String, Format>();
    private static Map<String, Codec> extensionToC = new HashMap<String, Codec>();
    private static Map<Format, Codec> videoCodecsForF = new HashMap<Format, Codec>();
    private static Map<Format, Codec> audioCodecsForF = new HashMap<Format, Codec>();

    static {
        profiles.add(new Avc2png());
        profiles.add(new Avc2prores());
        profiles.add(new MP4Jpeg2avc());
        profiles.add(new Mkv2png());
        profiles.add(new Mpeg2img());
        profiles.add(new Img2AvcMP4());
        profiles.add(new Png2mkv());
        profiles.add(new Png2prores());
        profiles.add(new Png2vp8());
        profiles.add(new Png2webm());
        profiles.add(new Prores2avc());
        profiles.add(new Prores2png());
        profiles.add(new Prores2vp8());
        profiles.add(new Prores2webm());
        profiles.add(new Ts2mp4());
        profiles.add(new TsAvc2Png());
        profiles.add(new Y4m2prores());
        profiles.add(new Webm2png());
        profiles.add(new MP42Wav());

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

        videoCodecsForF.put(Format.H264, Codec.H264);
        videoCodecsForF.put(Format.RAW, Codec.RAW);

        videoCodecsForF.put(Format.FLV, Codec.H264);

        videoCodecsForF.put(Format.AVI, Codec.MPEG4);

        videoCodecsForF.put(Format.IMG, Codec.PNG);

        videoCodecsForF.put(Format.MJPEG, Codec.JPEG);

        videoCodecsForF.put(Format.IVF, Codec.VP8);

        videoCodecsForF.put(Format.Y4M, Codec.RAW);

        videoCodecsForF.put(Format.WAV, Codec.PCM);

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

        String inputCodecVideoRaw = cmd.getStringFlag(FLAG_INPUT_VIDEO_CODEC);
        Codec inputCodecVideo = null;
        if (inputCodecVideoRaw == null) {
            if (inputFormat == Format.IMG) {
                inputCodecVideo = getCodecFromExtension(input);
            } else if (inputFormat.isVideo()) {
                inputCodecVideo = detectDecoderVideo(input, inputFormat);
            }
        } else {
            inputCodecVideo = Codec.valueOf(inputCodecVideoRaw.toUpperCase());
        }

        String outputCodecVideoRaw = cmd.getStringFlag(FLAG_OUTPUT_VIDEO_CODEC);
        Codec outputCodecVideo;
        if (outputCodecVideoRaw == null) {
            outputCodecVideo = getCodecFromExtension(output);
            if (outputCodecVideo == null)
                outputCodecVideo = getFirstVideoCodecForFormat(outputFormat);
        } else {
            outputCodecVideo = Codec.valueOf(outputCodecVideoRaw.toUpperCase());
        }

        String inputCodecAudioRaw = cmd.getStringFlag(FLAG_INPUT_AUDIO_CODEC);
        Codec inputCodecAudio = null;
        if (inputCodecAudioRaw == null) {
            if (inputFormat.isAudio()) {
                inputCodecAudio = detectDecoderAudio(input, inputFormat);
            }
        } else {
            inputCodecAudio = Codec.valueOf(inputCodecAudioRaw.toUpperCase());
        }

        String outputCodecAudioRaw = cmd.getStringFlag(FLAG_OUTPUT_AUDIO_CODEC);
        Codec outputCodecAudio = null;
        if (outputCodecAudioRaw == null) {
            if (outputFormat.isAudio())
                outputCodecAudio = getFirstAudioCodecForFormat(inputFormat);
        } else {
            outputCodecAudio = Codec.valueOf(outputCodecAudioRaw.toUpperCase());
        }
        if(inputCodecAudio == null)
            outputCodecAudio = null;

        List<Profile> candidates = new ArrayList<Profile>(profiles);
        for (Iterator<Profile> it = candidates.iterator(); it.hasNext();) {
            Profile next = it.next();
            if (!next.inputFormat().contains(inputFormat))
                it.remove();
        }
        for (Iterator<Profile> it = candidates.iterator(); it.hasNext();) {
            Profile next = it.next();
            if (!next.outputFormat().contains(outputFormat))
                it.remove();
        }

        if (inputCodecVideo != null) {
            for (Iterator<Profile> it = candidates.iterator(); it.hasNext();) {
                Profile next = it.next();
                if (next.inputVideoCodec() != null && !next.inputVideoCodec().contains(inputCodecVideo))
                    it.remove();
            }
        }

        if (outputCodecVideo != null) {
            for (Iterator<Profile> it = candidates.iterator(); it.hasNext();) {
                Profile next = it.next();
                if (next.outputVideoCodec() != null && !next.outputVideoCodec().contains(outputCodecVideo))
                    it.remove();
            }
        }

        if (inputCodecAudio != null) {
            for (Iterator<Profile> it = candidates.iterator(); it.hasNext();) {
                Profile next = it.next();
                if (next.inputAudioCodec() != null && !next.inputAudioCodec().contains(inputCodecAudio))
                    it.remove();
            }
        }

        if (outputCodecAudio != null) {
            for (Iterator<Profile> it = candidates.iterator(); it.hasNext();) {
                Profile next = it.next();
                if (next.outputAudioCodec() != null && !next.outputAudioCodec().contains(outputCodecAudio))
                    it.remove();
            }
        }

        if (candidates.size() == 0) {
            Logger.error("Transcoding profile not found for the parameters specified");
            return;
        }

        candidates.get(0).transcode(cmd);
    }

    private static Codec getFirstAudioCodecForFormat(Format inputFormat) {
        return audioCodecsForF.get(inputFormat);
    }

    private static Codec getFirstVideoCodecForFormat(Format inputFormat) {
        return videoCodecsForF.get(inputFormat);
    }

    private static Codec detectDecoderVideo(String input, Format format) throws IOException {
        Demuxer demuxer = JCodecUtil.createDemuxer(format, new File(input));
        List<? extends DemuxerTrack> video = demuxer.getVideoTracks();
        if (video.size() == 0)
            return null;
        Packet packet = video.get(0).nextFrame();
        if (packet == null)
            return null;

        return JCodecUtil.detectDecoder(packet.getData());
    }

    private static Codec detectDecoderAudio(String input, Format format) throws IOException {
        Demuxer demuxer = JCodecUtil.createDemuxer(format, new File(input));
        List<? extends DemuxerTrack> video = demuxer.getAudioTracks();
        if (video.size() == 0)
            return null;
        Packet packet = video.get(0).nextFrame();
        if (packet == null)
            return null;

        return JCodecUtil.detectDecoder(packet.getData());
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