package org.jcodec.api.transcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.api.transcode.filters.ColorTransformFilter;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.Tuple._3;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transcoder core.
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

    public Transcoder(String sourceName, String destName, Format inputFormat, Format outputFormat,
            _3<Integer, Integer, Codec> inputVideoCodec, Codec outputVideoCodec,
            _3<Integer, Integer, Codec> inputAudioCodec, Codec outputAudioCodec, boolean videoCodecCopy,
            boolean audioCodecCopy, List<Filter> extraFilters) {
        this.extraFilters = extraFilters;
        this.videoCodecCopy = videoCodecCopy;
        this.audioCodecCopy = audioCodecCopy;

        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;

        sink = new SinkImpl(destName, outputFormat, outputVideoCodec, outputAudioCodec);
        source = new SourceImpl(sourceName, inputFormat, inputVideoCodec, inputAudioCodec);
        // TODO(stan): check v+a -> audio only / video only
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

    public void setSeekFrames(Integer seekFrames) {
        this.seekFrames = seekFrames;
    }

    public void setMaxFrames(Integer integerFlagD) {
        this.maxFrames = integerFlagD;

    }

    public void setOption(Options option, Object value) {
        source.setOption(option, value);
        sink.setOption(option, value);

    }
}