package org.jcodec.api.transcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.api.transcode.filters.ColorTransformFilter;
import org.jcodec.common.Format;
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
 *   Transcoder.newTranscoder(source, sink).create();
 * The source and the sink are essential to the transcoder and must be provided.
 * 
 * @author The JCodec project
 * 
 */
public class Transcoder {
    static final int REORDER_BUFFER_SIZE = 7;

    private Source source;
    private Sink sink;
    private List<Filter> filters = new ArrayList<Filter>();
    private List<Filter> extraFilters;
    private int seekFrames;
    private int maxFrames;
    private boolean videoCodecCopy;
    private boolean audioCodecCopy;
    private Format inputFormat;
    private Format outputFormat;
    private boolean colorTransformInit;

    /**
     * Use TranscoderBuilder (method newTranscoder below) to create a transcoder
     * 
     * @param source
     * @param sink
     * @param videoCodecCopy
     * @param audioCodecCopy
     * @param extraFilters
     */
    private Transcoder(Source source, Sink sink, boolean videoCodecCopy, boolean audioCodecCopy,
            List<Filter> extraFilters, int seekFrames, int maxFrames) {
        this.extraFilters = extraFilters;
        this.videoCodecCopy = videoCodecCopy;
        this.audioCodecCopy = audioCodecCopy;

        this.inputFormat = source.getInputFormat();
        this.outputFormat = sink.getOutputFormat();
        this.seekFrames = seekFrames;
        this.maxFrames = maxFrames;
        
        this.source = source;
        this.sink = sink;
    }

    protected boolean audioCodecCopy() {
        return audioCodecCopy;
    }

    protected boolean videoCodecCopy() {
        return videoCodecCopy;
    }

    public void transcode() throws IOException {
        PixelStore pixelStore = new PixelStoreImpl();
        try {
            sink.init();

            source.init(pixelStore);
            source.seekFrames(seekFrames);

            if (outputFormat.isVideo()) {
                for (int frameNo = 0; frameNo <= maxFrames; frameNo++) {
                    if ((source instanceof PacketSource) && (sink instanceof PacketSink) && videoCodecCopy) {
                        PacketSource rawSource = (PacketSource) source;
                        PacketSink rawSink = (PacketSink) sink;
                        Packet videoPacket = rawSource.inputVideoPacket();
                        if (videoPacket == null)
                            break;
                        if (source.haveAudio() && outputFormat.isAudio()) {
                            double endPts = videoPacket.getPtsD() + 0.2;
                            outputAudioPacketsTo(endPts);
                        }
                        printLegend(frameNo, maxFrames, videoPacket);
                        rawSink.outputVideoPacket(videoPacket, source.getVideoCodecMeta());
                    } else {
                        VideoFrameWithPacket videoFrame = source.getNextVideoFrame();
                        if (videoFrame == null)
                            break;

                        if (!colorTransformInit && inputFormat.isVideo() && outputFormat.isVideo()) {
                            initColorTransform(videoFrame.getFrame().getColor());
                            colorTransformInit = true;
                        }

                        if (source.haveAudio() && outputFormat.isAudio()) {
                            double endPts = videoFrame.getPacket().getPtsD() + 0.2;
                            outputAudioPacketsTo(endPts);
                        }
                        Picture8Bit filteredFrame = videoFrame.getFrame();
                        if (!videoCodecCopy()) {
                            for (Filter filter : filters) {
                                Picture8Bit oldFrame = filteredFrame;
                                filteredFrame = filter.filter(filteredFrame, pixelStore);
                                pixelStore.putBack(oldFrame);
                            }
                        }
                        printLegend(frameNo, maxFrames, videoFrame.getPacket());

                        sink.outputVideoFrame(new VideoFrameWithPacket(videoFrame.getPacket(), filteredFrame));
                        pixelStore.putBack(filteredFrame);
                    }
                }
            } else if (source.haveAudio() && outputFormat.isAudio()) {
                outputAudioPacketsTo(Double.MAX_VALUE);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // Logger.error("Error in transcode: " + e.getMessage());
        } finally {
            source.finish();
            sink.finish();
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
        ColorSpace inputColor = sink.getInputColor();
        if (inputColor != null && inputColor != sourceColor)
            filters.add(new ColorTransformFilter(inputColor));
    }

    private void outputAudioPacketsTo(double endPts) throws IOException {
        Packet audioPacket;
        do {
            if ((source instanceof PacketSource) && (sink instanceof PacketSink) && audioCodecCopy) {
                audioPacket = ((PacketSource) source).inputAudioPacket();
                if (audioPacket == null)
                    break;
                ((PacketSink) sink).outputAudioPacket(audioPacket, source.getAudioCodecMeta());
            } else {
                AudioFrameWithPacket audioFrame = source.getNextAudioFrame();
                if (audioFrame == null)
                    break;
                audioPacket = audioFrame.getPacket();
                sink.outputAudioFrame(audioFrame);
            }
        } while (audioPacket.getPtsD() < endPts);
    }

    private void printLegend(int frameNo, int maxFrames, Packet inVideoPacket) {
        if (frameNo % 100 == 0)
            System.out.print(String.format("[%6d]\r", frameNo));
    }

    public static class TranscoderBuilder {

        private Source source;
        private Sink sink;
        private boolean videoCopy;
        private boolean audioCopy;
        private List<Filter> filters = new ArrayList<Filter>();
        private int seekFrames;
        private int maxFrames;

        public TranscoderBuilder(Source source, Sink sink) {
            this.source = source;
            this.sink = sink;
        }

        public TranscoderBuilder setVideoCopy(boolean videoCopy) {
            this.videoCopy = videoCopy;
            return this;
        }

        public TranscoderBuilder setAudioCopy(boolean audioCopy) {
            this.audioCopy = audioCopy;
            return this;
        }

        public TranscoderBuilder addFilters(List<Filter> filters) {
            this.filters.addAll(filters);
            return this;
        }

        public Transcoder create() {
            return new Transcoder(source, sink, videoCopy, audioCopy, filters, seekFrames, maxFrames);
        }

        public TranscoderBuilder setSeekFrames(int seekFrames) {
            this.seekFrames = seekFrames;
            return this;
        }

        public TranscoderBuilder setMaxFrames(int maxFrames) {
            this.maxFrames = maxFrames;
            return this;
        }
    }

    public static TranscoderBuilder newTranscoder(Source source, Sink sink) {
        return new TranscoderBuilder(source, sink);
    }
}