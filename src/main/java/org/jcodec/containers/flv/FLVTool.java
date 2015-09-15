package org.jcodec.containers.flv;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.IOUtils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.StringUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.flv.FLVTag.AudioTagHeader;
import org.jcodec.containers.flv.FLVTag.AvcVideoTagHeader;
import org.jcodec.containers.flv.FLVTag.Type;
import org.jcodec.containers.flv.FLVTag.VideoTagHeader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Makes a clip out of an FLV
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class FLVTool {
    private static Map<String, PacketProcessorFactory> processors = new HashMap<String, PacketProcessorFactory>() {
        {
            put("clip", new ClipPacketProcessor.Factory());
            put("fix_pts", new FixPtsProcessor.Factory());
            put("info", new InfoPacketProcessor.Factory());
        }
    };

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.args.length < 2) {
            if (cmd.args.length > 0) {
                MainUtils.printHelp(processors.get(cmd.getArg(0)).getFlags(), cmd.getArg(0), "file in", "?file out");
            } else {
                printGenericHelp();
            }
            return;
        }
        String command = cmd.getArg(0);

        PacketProcessor processor = getProcessor(command, cmd);
        if (processor == null) {
            System.err.println("Unknown command: " + command);
            printGenericHelp();
            return;
        }

        SeekableByteChannel in = null;
        SeekableByteChannel out = null;
        try {
            in = NIOUtils.readableFileChannel(new File(cmd.getArg(1)));
            if (processor.hasOutput())
                out = NIOUtils.writableFileChannel(new File(cmd.getArg(2)));
            FLVReader demuxer = new FLVReader(in);
            FLVWriter muxer = new FLVWriter(out);
            FLVTag pkt = null;
            while ((pkt = demuxer.readNextPacket()) != null) {
                if (!processor.processPacket(pkt, muxer))
                    break;
            }
            processor.finish(muxer);
            muxer.finish();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private static void printGenericHelp() {
        System.err.println("Syntax: <command> [flags] <file in> <file out>\nWhere command is: ["
                + StringUtils.join(processors.keySet().toArray(new String[0]), ",") + "].");
    }

    private static PacketProcessor getProcessor(String command, Cmd cmd) {
        PacketProcessorFactory factory = processors.get(command);
        if (factory == null)
            return null;
        return factory.newPacketProcessor(cmd);
    }

    public static interface PacketProcessor {
        boolean processPacket(FLVTag pkt, FLVWriter writer) throws IOException;

        boolean hasOutput();

        void finish(FLVWriter muxer) throws IOException;
    }

    public static interface PacketProcessorFactory {
        PacketProcessor newPacketProcessor(Cmd flags);

        Map<String, String> getFlags();
    }

    /**
     * A packet processor that clips the flv between the given timestamps
     * 
     */
    public static class ClipPacketProcessor implements PacketProcessor {
        private static FLVTag h264Config;
        private boolean copying = false;
        private Double from;
        private Double to;

        public static class Factory implements PacketProcessorFactory {
            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new ClipPacketProcessor(flags.getDoubleFlag("from"), flags.getDoubleFlag("to"));
            }

            @Override
            public Map<String, String> getFlags() {
                return new HashMap<String, String>() {
                    {
                        put("from", "From timestamp (in seconds, i.e 67.49)");
                        put("to", "To timestamp");

                    }
                };
            }
        }

        public ClipPacketProcessor(Double from, Double to) {
            this.from = from;
            this.to = to;
        }

        public boolean processPacket(FLVTag pkt, FLVWriter writer) throws IOException {
            if (pkt.getType() == Type.VIDEO && pkt.getTagHeader().getCodec() == Codec.H264) {
                if (((AvcVideoTagHeader) pkt.getTagHeader()).getAvcPacketType() == 0) {
                    h264Config = pkt;
                    System.out.println("GOT AVCC");
                }
            }

            if (!copying && (from == null || pkt.getPtsD() > from) && pkt.getType() == Type.VIDEO && pkt.isKeyFrame()
                    && h264Config != null) {
                System.out.println("Starting at packet: " + ToJSON.toJSON(pkt));
                copying = true;
                h264Config.setPts(pkt.getPts());
                writer.addPacket(h264Config);
            }

            if ((to != null && pkt.getPtsD() >= to)) {
                System.out.println("Stopping at packet: " + ToJSON.toJSON(pkt));
                return false;
            }
            if (copying)
                writer.addPacket(pkt);
            return true;
        }

        @Override
        public void finish(FLVWriter muxer) {
        }

        @Override
        public boolean hasOutput() {
            return true;
        }
    }

    /**
     * A packet processor that forces a certain FPS
     * 
     */
    public static class FixPtsProcessor implements PacketProcessor {
        private double lastPtsAudio = 0;
        private double lastPtsVideo = 0;
        private List<FLVTag> tags = new ArrayList<FLVTag>();
        private int audioTagsInQueue;
        private int videoTagsInQueue;
        private static final double CORRECTION_PACE = 0.33;

        public static class Factory implements PacketProcessorFactory {
            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new FixPtsProcessor();
            }

            @Override
            public Map<String, String> getFlags() {
                return new HashMap<String, String>();
            }
        }

        public FixPtsProcessor() {
        }

        public boolean processPacket(FLVTag pkt, FLVWriter writer) throws IOException {
            tags.add(pkt);
            if (pkt.getType() == Type.AUDIO) {
                ++audioTagsInQueue;
            } else if (pkt.getType() == Type.VIDEO) {
                ++videoTagsInQueue;
            }

            if (tags.size() < 600)
                return true;

            processOneTag(writer);
            return true;
        }

        private void processOneTag(FLVWriter writer) throws IOException {
            FLVTag tag = tags.remove(0);

            if (tag.getType() == Type.AUDIO) {
                tag.setPts((int) Math.round(lastPtsAudio * 1000));
                lastPtsAudio += audioFrameDuration(((AudioTagHeader) tag.getTagHeader()));
                --audioTagsInQueue;
            } else if (tag.getType() == Type.VIDEO) {
                double duration = (((double) 1024 * audioTagsInQueue) / (48000 * videoTagsInQueue));
                tag.setPts((int) Math.round(lastPtsVideo * 1000));
                lastPtsVideo += min(
                        (1 + CORRECTION_PACE) * duration,
                        max((1 - CORRECTION_PACE) * duration, duration + min(1, abs(lastPtsAudio - lastPtsVideo))
                                * (lastPtsAudio - lastPtsVideo)));
                --videoTagsInQueue;
                System.out.println(lastPtsVideo + " - " + lastPtsAudio);
            } else {
                tag.setPts((int) Math.round(lastPtsVideo * 1000));
            }

            writer.addPacket(tag);
        }

        private double audioFrameDuration(AudioTagHeader audioTagHeader) {
            switch (audioTagHeader.getCodec()) {
            case AAC:
                return ((double) 1024) / audioTagHeader.getAudioFormat().getSampleRate();
            case MP3:
                return ((double) 1152) / audioTagHeader.getAudioFormat().getSampleRate();
            default:
                throw new RuntimeException("Audio codec:" + audioTagHeader.getCodec() + " is not supported.");
            }
        }

        @Override
        public void finish(FLVWriter muxer) throws IOException {
            while (tags.size() > 0) {
                processOneTag(muxer);
            }
        }

        @Override
        public boolean hasOutput() {
            return true;
        }
    }

    /**
     * A packet processor that just dumps info
     * 
     */
    public static class InfoPacketProcessor implements PacketProcessor {

        public static class Factory implements PacketProcessorFactory {
            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new InfoPacketProcessor();
            }

            @Override
            public Map<String, String> getFlags() {
                return new HashMap<String, String>() {
                    {
                    }
                };
            }
        }

        @Override
        public boolean processPacket(FLVTag pkt, FLVWriter writer) throws IOException {
            System.out.print("T=" + typeString(pkt.getType()) + "|PTS=" + pkt.getPts() + "|"
                    + (pkt.isKeyFrame() ? "K" : " ") + "|POS=" + pkt.getPosition());
            if (pkt.getTagHeader() instanceof VideoTagHeader) {
                VideoTagHeader vt = (VideoTagHeader) pkt.getTagHeader();
                System.out.print("|C=" + vt.getCodec() + "|FT=" + vt.getFrameType());
                if (vt instanceof AvcVideoTagHeader) {
                    AvcVideoTagHeader avct = (AvcVideoTagHeader) vt;
                    System.out.print("|PKT_TYPE=" + avct.getAvcPacketType() + "|COMP_OFF=" + avct.getCompOffset());
                    if (avct.getAvcPacketType() == 0) {
                        ByteBuffer frameData = pkt.getData().duplicate();
                        FLVReader.parseVideoTagHeader(frameData);
                        AvcCBox avcc = H264Utils.parseAVCC(frameData);
                        for (SeqParameterSet sps : H264Utils.readSPS(avcc.getSpsList())) {
                            System.out.println();
                            System.out.print("  SPS[" + sps.getSeq_parameter_set_id() + "]:" + ToJSON.toJSON(sps));
                        }
                        for (PictureParameterSet pps : H264Utils.readPPS(avcc.getPpsList())) {
                            System.out.println();
                            System.out.print("  PPS[" + pps.getPic_parameter_set_id() + "]:" + ToJSON.toJSON(pps));
                        }
                    }
                }
            } else if (pkt.getTagHeader() instanceof AudioTagHeader) {
                AudioTagHeader at = (AudioTagHeader) pkt.getTagHeader();
                AudioFormat format = at.getAudioFormat();
                System.out.print("|C=" + at.getCodec() + "|SR=" + format.getSampleRate() + "|SS="
                        + (format.getSampleSizeInBits() >> 3) + "|CH=" + format.getChannels());
            } else if (pkt.getType() == Type.SCRIPT) {
                FLVMetadata metadata = FLVReader.parseMetadata(pkt.getData().duplicate());
                if (metadata != null) {
                    System.out.println();
                    System.out.print("  Metadata:" + ToJSON.toJSON(metadata));
                }
            }
            System.out.println();
            return true;
        }

        private String typeString(Type type) {
            return type.toString().substring(0, 1);
        }

        @Override
        public void finish(FLVWriter muxer) throws IOException {
        }

        @Override
        public boolean hasOutput() {
            return false;
        }
    }
}