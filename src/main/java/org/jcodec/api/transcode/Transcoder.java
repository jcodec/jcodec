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
//    private Format inputFormat;
//    private Format outputFormat;
    private boolean colorTransformInit;
    private List<Mapping> videoMappings;
    private List<Mapping> audioMappings;

    /**
     * Use TranscoderBuilder (method newTranscoder below) to create a transcoder
     * 
     * @param source
     * @param sink
     * @param videoCodecCopy
     * @param audioCodecCopy
     * @param extraFilters
     */
    private Transcoder(Source[] source, Sink[] sink, List<Mapping> videoMappings, List<Mapping> audioMappings,
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
        private Source source;
        private Sink sink;
        private boolean copy;

        public Mapping(Source source, Sink sink, boolean copy) {
            this.source = source;
            this.sink = sink;
            this.copy = copy;
        }

        public boolean isCopy() {
            return copy;
        }
    }

    public void transcode() throws IOException {
        PixelStore pixelStore = new PixelStoreImpl();
        try {
            sinks[0].init();

            sources[0].init(pixelStore);
            sources[0].seekFrames(seekFrames);

            if (sinks[0].isVideo()) {
                for (int frameNo = 0; frameNo <= maxFrames; frameNo++) {
                    if ((sources[0] instanceof PacketSource) && (sinks[0] instanceof PacketSink)
                            && videoMappings.get(0).isCopy()) {
                        PacketSource rawSource = (PacketSource) sources[0];
                        PacketSink rawSink = (PacketSink) sinks[0];
                        Packet videoPacket = rawSource.inputVideoPacket();
                        if (videoPacket == null)
                            break;
                        if (sources[0].haveAudio() && sinks[0].isAudio()) {
                            double endPts = videoPacket.getPtsD() + 0.2;
                            outputAudioPacketsTo(endPts);
                        }
                        printLegend(frameNo, maxFrames, videoPacket);
                        rawSink.outputVideoPacket(videoPacket, sources[0].getVideoCodecMeta());
                    } else {
                        VideoFrameWithPacket videoFrame = sources[0].getNextVideoFrame();
                        if (videoFrame == null)
                            break;

                        if (!colorTransformInit && sources[0].isVideo() && sinks[0].isVideo()) {
                            initColorTransform(videoFrame.getFrame().getColor());
                            colorTransformInit = true;
                        }

                        if (sources[0].haveAudio() && sinks[0].isAudio()) {
                            double endPts = videoFrame.getPacket().getPtsD() + 0.2;
                            outputAudioPacketsTo(endPts);
                        }
                        Picture8Bit filteredFrame = videoFrame.getFrame();
                        if (!videoMappings.get(0).isCopy()) {
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
            } else if (sources[0].haveAudio() && sinks[0].isAudio()) {
                outputAudioPacketsTo(Double.MAX_VALUE);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // Logger.error("Error in transcode: " + e.getMessage());
        } finally {
            sources[0].finish();
            sinks[0].finish();
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

    private void outputAudioPacketsTo(double endPts) throws IOException {
        Packet audioPacket;
        do {
            if ((sources[0] instanceof PacketSource) && (sinks[0] instanceof PacketSink)
                    && audioMappings.get(0).isCopy()) {
                audioPacket = ((PacketSource) sources[0]).inputAudioPacket();
                if (audioPacket == null)
                    break;
                ((PacketSink) sinks[0]).outputAudioPacket(audioPacket, sources[0].getAudioCodecMeta());
            } else {
                AudioFrameWithPacket audioFrame = sources[0].getNextAudioFrame();
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
            return this;
        }

        public TranscoderBuilder addVideoMapping(Source src, Sink sink, boolean copy) {
            videoMappings.add(new Mapping(src, sink, copy));
            return this;
        }

        public TranscoderBuilder addAudioMapping(Source src, Sink sink, boolean copy) {
            audioMappings.add(new Mapping(src, sink, copy));
            return this;
        }

        public Transcoder create() {
            return new Transcoder(source.toArray(new Source[] {}), sink.toArray(new Sink[] {}), videoMappings,
                    audioMappings, filters, seekFrames, maxFrames);
        }
    }

    public static TranscoderBuilder newTranscoder() {
        return new TranscoderBuilder();
    }
}