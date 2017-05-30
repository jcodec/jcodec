package org.jcodec.api.transcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.api.transcode.filters.ColorTransformFilter;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transcoder core.
 * 
 * The simplest way to create a transcoder with default options:
 * Transcoder.newTranscoder(source, sink).create(); The source and the sink are
 * essential to the transcoder and must be provided.
 * 
 * @author The JCodec project
 * 
 */
public class Transcoder {
    static final int REORDER_BUFFER_SIZE = 7;

    private Source[] sources;
    private Sink[] sinks;
    private List<Filter> filters = new ArrayList<Filter>();
    private List<Filter> extraFilters;
    private int seekFrames;
    private int maxFrames;
    private boolean colorTransformInit;
    private Mapping[] videoMappings;
    private Mapping[] audioMappings;

    /**
     * Use TranscoderBuilder (method newTranscoder below) to create a transcoder
     * 
     * @param source
     * @param sink
     * @param videoCodecCopy
     * @param audioCodecCopy
     * @param extraFilters
     */
    private Transcoder(Source[] source, Sink[] sink, Mapping[] videoMappings, Mapping[] audioMappings,
            List<Filter> extraFilters, int seekFrames, int maxFrames) {
        this.extraFilters = extraFilters;
        this.videoMappings = videoMappings;
        this.audioMappings = audioMappings;

        this.seekFrames = seekFrames;
        this.maxFrames = maxFrames;

        this.sources = source;
        this.sinks = sink;
    }

    private static class Mapping {
        private int source;
        private boolean copy;

        public Mapping(int source, boolean copy) {
            this.source = source;
            this.copy = copy;
        }

        public boolean isCopy() {
            return copy;
        }
    }

    public void transcode() throws IOException {
        PixelStore pixelStore = new PixelStoreImpl();
        try {
            for (int i = 0; i < sinks.length; i++)
                sinks[i].init();

            for (int i = 0; i < sources.length; i++) {
                sources[i].init(pixelStore);
                sources[i].seekFrames(seekFrames);
            }
            Source audioSource = sources[audioMappings[0].source];
            Source videoSource = sources[videoMappings[0].source];
            boolean audioCopy = audioMappings[0].isCopy();
            boolean videoCopy = videoMappings[0].isCopy();

            if (sinks[0].isVideo()) {
                for (int frameNo = 0; frameNo <= maxFrames; frameNo++) {
                    if ((videoSource instanceof PacketSource) && (sinks[0] instanceof PacketSink) && videoCopy) {
                        PacketSource rawSource = (PacketSource) videoSource;
                        PacketSink rawSink = (PacketSink) sinks[0];
                        Packet videoPacket = rawSource.inputVideoPacket();
                        if (videoPacket == null)
                            break;
                        if (audioSource.haveAudio() && sinks[0].isAudio()) {
                            double endPts = videoPacket.getPtsD() + 0.2;
                            outputAudioPacketsTo(audioSource, audioCopy, endPts);
                        }
                        printLegend(frameNo, maxFrames, videoPacket);
                        rawSink.outputVideoPacket(videoPacket, videoSource.getVideoCodecMeta());
                    } else {
                        VideoFrameWithPacket videoFrame = videoSource.getNextVideoFrame();
                        if (videoFrame == null)
                            break;

                        if (!colorTransformInit && videoSource.isVideo() && sinks[0].isVideo()) {
                            initColorTransform(videoFrame.getFrame().getColor());
                            colorTransformInit = true;
                        }

                        if (audioSource.haveAudio() && sinks[0].isAudio()) {
                            double endPts = videoFrame.getPacket().getPtsD() + 0.2;
                            outputAudioPacketsTo(audioSource, audioCopy, endPts);
                        }
                        Picture8Bit filteredFrame = videoFrame.getFrame();
                        if (!videoCopy) {
                            for (Filter filter : filters) {
                                Picture8Bit oldFrame = filteredFrame;
                                filteredFrame = filter.filter(filteredFrame, pixelStore);
                                pixelStore.putBack(oldFrame);
                            }
                        }
                        printLegend(frameNo, maxFrames, videoFrame.getPacket());

                        sinks[0].outputVideoFrame(new VideoFrameWithPacket(videoFrame.getPacket(), filteredFrame));
                        pixelStore.putBack(filteredFrame);
                    }
                }
            } else if (audioSource.haveAudio() && sinks[0].isAudio()) {
                outputAudioPacketsTo(audioSource, audioCopy, Double.MAX_VALUE);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // Logger.error("Error in transcode: " + e.getMessage());
        } finally {
            for (int i = 0; i < sources.length; i++)
                sources[0].finish();
            for (int i = 0; i < sinks.length; i++)
                sinks[i].finish();
        }
    }

    private void initColorTransform(ColorSpace sourceColor) {
        for (Filter filter : extraFilters) {
            ColorSpace inputColor = filter.getInputColor();
            if (inputColor != null && inputColor != sourceColor) {
                filters.add(new ColorTransformFilter(inputColor));
            }
            filters.add(filter);
            sourceColor = filter.getOutputColor();
        }
        ColorSpace inputColor = sinks[0].getInputColor();
        if (inputColor != null && inputColor != sourceColor)
            filters.add(new ColorTransformFilter(inputColor));
    }

    private void outputAudioPacketsTo(Source audioSource, boolean audioCopy, double endPts) throws IOException {
        Packet audioPacket;
        do {
            if ((audioSource instanceof PacketSource) && (sinks[0] instanceof PacketSink) && audioCopy) {
                audioPacket = ((PacketSource) audioSource).inputAudioPacket();
                if (audioPacket == null)
                    break;
                ((PacketSink) sinks[0]).outputAudioPacket(audioPacket, audioSource.getAudioCodecMeta());
            } else {
                AudioFrameWithPacket audioFrame = audioSource.getNextAudioFrame();
                if (audioFrame == null)
                    break;
                audioPacket = audioFrame.getPacket();
                sinks[0].outputAudioFrame(audioFrame);
            }
        } while (audioPacket.getPtsD() < endPts);
    }

    private void printLegend(int frameNo, int maxFrames, Packet inVideoPacket) {
        if (frameNo % 100 == 0)
            System.out.print(String.format("[%6d]\r", frameNo));
    }

    public static class TranscoderBuilder {

        private List<Source> source = new ArrayList<Source>();
        private List<Sink> sink = new ArrayList<Sink>();
        private List<Filter> filters = new ArrayList<Filter>();
        private List<Mapping> videoMappings = new ArrayList<Mapping>();
        private List<Mapping> audioMappings = new ArrayList<Mapping>();
        private int seekFrames;
        private int maxFrames;

        public TranscoderBuilder() {
        }

        public TranscoderBuilder addFilters(List<Filter> filters) {
            this.filters.addAll(filters);
            return this;
        }

        public TranscoderBuilder setSeekFrames(int seekFrames) {
            this.seekFrames = seekFrames;
            return this;
        }

        public TranscoderBuilder setMaxFrames(int maxFrames) {
            this.maxFrames = maxFrames;
            return this;
        }

        public TranscoderBuilder addSource(Source source) {
            this.source.add(source);
            return this;
        }

        public TranscoderBuilder addSink(Sink sink) {
            this.sink.add(sink);
            videoMappings.add(new Mapping(0, false));
            audioMappings.add(new Mapping(0, false));
            return this;
        }

        public TranscoderBuilder setVideoMapping(int src, int sink, boolean copy) {
            videoMappings.set(sink, new Mapping(src, copy));
            return this;
        }

        public TranscoderBuilder setAudioMapping(int src, int sink, boolean copy) {
            audioMappings.set(sink, new Mapping(src, copy));
            return this;
        }

        public Transcoder create() {
            return new Transcoder(source.toArray(new Source[] {}), sink.toArray(new Sink[] {}),
                    videoMappings.toArray(new Mapping[] {}), audioMappings.toArray(new Mapping[] {}), filters,
                    seekFrames, maxFrames);
        }
    }

    public static TranscoderBuilder newTranscoder() {
        return new TranscoderBuilder();
    }
}