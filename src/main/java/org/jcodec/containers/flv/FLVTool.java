package org.jcodec.containers.flv;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.StringUtils;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.flv.FLVTag.AacAudioTagHeader;
import org.jcodec.containers.flv.FLVTag.AudioTagHeader;
import org.jcodec.containers.flv.FLVTag.AvcVideoTagHeader;
import org.jcodec.containers.flv.FLVTag.Type;
import org.jcodec.containers.flv.FLVTag.VideoTagHeader;
import org.jcodec.platform.Platform;

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
    private static Map<String, PacketProcessorFactory> processors = new HashMap<String, PacketProcessorFactory>();
    static {
        processors.put("clip", new ClipPacketProcessor.Factory());
        processors.put("fix_pts", new FixPtsProcessor.Factory());
        processors.put("info", new InfoPacketProcessor.Factory());
        processors.put("shift_pts", new ShiftPtsProcessor.Factory());
    }

    private static final Flag FLAG_MAX_PACKETS = new Flag("max-packets", "m", "Maximum number of packets to process");

    public static void main1(String[] args) throws IOException {
        if (args.length < 1) {
            printGenericHelp();
            return;
        }
        String command = args[0];
        PacketProcessorFactory processorFactory = processors.get(command);
        if (processorFactory == null) {
            System.err.println("Unknown command: " + command);
            printGenericHelp();
            return;
        }

        Cmd cmd = MainUtils.parseArguments(Platform.copyOfRangeO(args, 1, args.length), processorFactory.getFlags());
        if (cmd.args.length < 1) {
            MainUtils.printHelpCmd(command, processorFactory.getFlags(), asList("file in", "?file out"));
            return;
        }
        PacketProcessor processor = processorFactory.newPacketProcessor(cmd);
        int maxPackets = cmd.getIntegerFlagD(FLAG_MAX_PACKETS, Integer.MAX_VALUE);

        SeekableByteChannel _in = null;
        SeekableByteChannel out = null;
        try {
            _in = NIOUtils.readableChannel(new File(cmd.getArg(0)));
            if (processor.hasOutput())
                out = NIOUtils.writableChannel(new File(cmd.getArg(1)));
            FLVReader demuxer = new FLVReader(_in);
            FLVWriter muxer = new FLVWriter(out);
            FLVTag pkt = null;
            for (int i = 0; i < maxPackets && (pkt = demuxer.readNextPacket()) != null; i++) {
                if (!processor.processPacket(pkt, muxer))
                    break;
            }
            processor.finish(muxer);
            if (processor.hasOutput())
                muxer.finish();
        } finally {
            IOUtils.closeQuietly(_in);
            IOUtils.closeQuietly(out);
        }
    }

    private static void printGenericHelp() {
        System.err.println("Syntax: <command> [flags] <file in> [file out]\nWhere command is: ["
                + StringUtils.joinS(processors.keySet().toArray(new String[0]), ", ") + "].");
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

        Flag[] getFlags();
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

        private static final Flag FLAG_FROM = new Flag("from", "From timestamp (in seconds, i.e 67.49)");
        private static final Flag FLAG_TO = new Flag("to", "To timestamp");

        public static class Factory implements PacketProcessorFactory {
            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new ClipPacketProcessor(flags.getDoubleFlag(FLAG_FROM), flags.getDoubleFlag(FLAG_TO));
            }

            @Override
            public Flag[] getFlags() {
                return new Flag[] { FLAG_FROM, FLAG_TO };
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
        private List<FLVTag> tags;
        private int audioTagsInQueue;
        private int videoTagsInQueue;
        private static final double CORRECTION_PACE = 0.33;

        public static class Factory implements PacketProcessorFactory {
            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new FixPtsProcessor();
            }

            @Override
            public Flag[] getFlags() {
                return new Flag[] {};
            }
        }

        public FixPtsProcessor() {
            this.tags = new ArrayList<FLVTag>();
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
                lastPtsVideo += min((1 + CORRECTION_PACE) * duration, max((1 - CORRECTION_PACE) * duration,
                        duration + min(1, abs(lastPtsAudio - lastPtsVideo)) * (lastPtsAudio - lastPtsVideo)));
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
        private FLVTag prevVideoTag;
        private FLVTag prevAudioTag;

        public static class Factory implements PacketProcessorFactory {
            private static final Flag FLAG_CHECK = new Flag("check",
                    "Check sanity and report errors only, no packet dump will be generated.");
            private static final Flag FLAG_STREAM = new Flag("stream",
                    "Stream selector, can be one of: ['video', 'audio', 'script'].");

            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new InfoPacketProcessor(flags.getBooleanFlagD(FLAG_CHECK, false),
                        flags.getEnumFlagD(FLAG_STREAM, null, Type.class));
            }

            @Override
            public Flag[] getFlags() {
                return new Flag[] { FLAG_CHECK, FLAG_STREAM };
            }
        }

        private boolean checkOnly;
        private Type streamType;

        public InfoPacketProcessor(boolean checkOnly, Type streamType) {
            this.checkOnly = checkOnly;
            this.streamType = streamType;
        }

        @Override
        public boolean processPacket(FLVTag pkt, FLVWriter writer) throws IOException {
            if (checkOnly)
                return true;
            if (pkt.getType() == Type.VIDEO) {
                if (streamType == Type.VIDEO || streamType == null) {
                    if (prevVideoTag != null)
                        dumpOnePacket(prevVideoTag, pkt.getPts() - prevVideoTag.getPts());
                    prevVideoTag = pkt;
                }
            } else if (pkt.getType() == Type.AUDIO) {
                if (streamType == Type.AUDIO || streamType == null) {
                    if (prevAudioTag != null)
                        dumpOnePacket(prevAudioTag, pkt.getPts() - prevAudioTag.getPts());
                    prevAudioTag = pkt;
                }
            } else {
                dumpOnePacket(pkt, 0);
            }

            return true;
        }

        private void dumpOnePacket(FLVTag pkt, int duration) {
            System.out.print("T=" + typeString(pkt.getType()) + "|PTS=" + pkt.getPts() + "|DUR=" + duration + "|"
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
                        AvcCBox avcc = H264Utils.parseAVCCFromBuffer(frameData);
                        for (SeqParameterSet sps : H264Utils.readSPSFromBufferList(avcc.getSpsList())) {
                            System.out.println();
                            System.out.print("  SPS[" + sps.getSeq_parameter_set_id() + "]:" + ToJSON.toJSON(sps));
                        }
                        for (PictureParameterSet pps : H264Utils.readPPSFromBufferList(avcc.getPpsList())) {
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
        }

        private String typeString(Type type) {
            return type.toString().substring(0, 1);
        }

        @Override
        public void finish(FLVWriter muxer) throws IOException {
            if (prevVideoTag != null)
                dumpOnePacket(prevVideoTag, 0);
            if (prevAudioTag != null)
                dumpOnePacket(prevAudioTag, 0);
        }

        @Override
        public boolean hasOutput() {
            return false;
        }
    }

    /**
     * A packet processor shifts pts
     * 
     */
    public static class ShiftPtsProcessor implements PacketProcessor {

        private static final long WRAP_AROUND_VALUE = 0x80000000L;
        private static final int HALF_WRAP_AROUND_VALUE = 0x40000000;

        private static final Flag FLAG_TO = new Flag("to",
                "Shift first pts to this value, and all subsequent pts accordingly.");
        private static final Flag FLAG_BY = new Flag("by", "Shift all pts by this value.");
        private static final Flag FLAG_WRAP_AROUND = new Flag("wrap-around", "Expect wrap around of timestamps.");

        public static class Factory implements PacketProcessorFactory {
            @Override
            public PacketProcessor newPacketProcessor(Cmd flags) {
                return new ShiftPtsProcessor(flags.getIntegerFlagD(FLAG_TO, 0), flags.getIntegerFlag(FLAG_BY),
                        flags.getBooleanFlagD(FLAG_WRAP_AROUND, false));
            }

            @Override
            public Flag[] getFlags() {
                return new Flag[] { FLAG_TO, FLAG_BY, FLAG_WRAP_AROUND };
            }
        }

        private int shiftTo;
        private Integer shiftBy;
        private long ptsDelta;
        private boolean firstPtsSeen;
        private List<FLVTag> savedTags;
        private boolean expectWrapAround;
        private int prevPts;

        public ShiftPtsProcessor(int shiftTo, Integer shiftBy, boolean expectWrapAround) {
            this.savedTags = new LinkedList<FLVTag>();
            this.shiftTo = shiftTo;
            this.shiftBy = shiftBy;
            this.expectWrapAround = true;
        }

        public boolean processPacket(FLVTag pkt, FLVWriter writer) throws IOException {
            boolean avcPrivatePacket = pkt.getType() == Type.VIDEO
                    && ((VideoTagHeader) pkt.getTagHeader()).getCodec() == Codec.H264
                    && ((AvcVideoTagHeader) pkt.getTagHeader()).getAvcPacketType() == 0;
            boolean aacPrivatePacket = pkt.getType() == Type.AUDIO
                    && ((AudioTagHeader) pkt.getTagHeader()).getCodec() == Codec.AAC
                    && ((AacAudioTagHeader) pkt.getTagHeader()).getPacketType() == 0;

            boolean validPkt = pkt.getType() != Type.SCRIPT && !avcPrivatePacket && !aacPrivatePacket;
            if (expectWrapAround && validPkt && pkt.getPts() < prevPts
                    && ((long) prevPts - pkt.getPts() > HALF_WRAP_AROUND_VALUE)) {
                Logger.warn("Wrap around detected: " + prevPts + " -> " + pkt.getPts());

                if (pkt.getPts() < -HALF_WRAP_AROUND_VALUE) {
                    ptsDelta += (WRAP_AROUND_VALUE << 1);
                } else if (pkt.getPts() >= 0) {
                    ptsDelta += WRAP_AROUND_VALUE;
                }
            }
            if (validPkt)
                prevPts = pkt.getPts();

            if (firstPtsSeen) {
                writePacket(pkt, writer);
            } else {
                if (!validPkt) {
                    savedTags.add(pkt);
                } else {
                    if (shiftBy != null) {
                        ptsDelta = shiftBy;
                        if (ptsDelta + pkt.getPts() < 0)
                            ptsDelta = -pkt.getPts();
                    } else {
                        ptsDelta = shiftTo - pkt.getPts();
                    }
                    firstPtsSeen = true;
                    emptySavedTags(writer);
                    writePacket(pkt, writer);
                }
            }

            return true;
        }

        private void writePacket(FLVTag pkt, FLVWriter writer) throws IOException {
            long newPts = pkt.getPts() + ptsDelta;
            if (newPts < 0) {
                Logger.warn("Preventing negative pts for tag @" + pkt.getPosition());
                if (shiftBy != null)
                    newPts = 0;
                else
                    newPts = shiftTo;
            } else if (newPts >= WRAP_AROUND_VALUE) {
                Logger.warn("PTS wrap around @" + pkt.getPosition());
                newPts -= WRAP_AROUND_VALUE;
                ptsDelta = newPts - pkt.getPts();
            }
            pkt.setPts((int) newPts);
            writer.addPacket(pkt);
        }

        private void emptySavedTags(FLVWriter muxer) throws IOException {
            while (savedTags.size() > 0) {
                writePacket(savedTags.remove(0), muxer);
            }
        }

        @Override
        public void finish(FLVWriter muxer) throws IOException {
            emptySavedTags(muxer);
        }

        @Override
        public boolean hasOutput() {
            return true;
        }
    }
}