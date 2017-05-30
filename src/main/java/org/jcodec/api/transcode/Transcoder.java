package org.jcodec.api.transcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.api.transcode.filters.ColorTransformFilter;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;

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
    private List<Filter>[] extraFilters;
    private int[] seekFrames;
    private int[] maxFrames;
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
            List<Filter>[] extraFilters, int[] seekFrames, int[] maxFrames) {
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
    }

    private static class Stream {
        private static final double AUDIO_LEADING_TIME = .2;
        private LinkedList<VideoFrameWithPacket> videoQueue = new LinkedList<VideoFrameWithPacket>();
        private LinkedList<AudioFrameWithPacket> audioQueue = new LinkedList<AudioFrameWithPacket>();
        private List<Filter> filters;
        private List<Filter> extraFilters;
        private Sink sink;
        private boolean videoCopy;
        private boolean audioCopy;
        private PixelStore pixelStore;
        private VideoCodecMeta videoCodecMeta;
        private AudioCodecMeta audioCodecMeta;
        private static final int REORDER_LENGTH = 5;

        public Stream(Sink sink, boolean videoCopy, boolean audioCopy, List<Filter> extraFilters,
                PixelStore pixelStore) {
            this.sink = sink;
            this.videoCopy = videoCopy;
            this.audioCopy = audioCopy;
            this.extraFilters = extraFilters;
            this.pixelStore = pixelStore;
        }

        private List<Filter> initColorTransform(ColorSpace sourceColor, List<Filter> extraFilters, Sink sink) {
            ArrayList<Filter> filters = new ArrayList<Filter>();
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
            return filters;
        }

        public void tryFlushQueues() throws IOException {
            // Do we have enough audio
            if (videoQueue.size() <= 0)
                return;
            if (videoCopy && videoQueue.size() < REORDER_LENGTH)
                return;
            if (!haveEnoughAudio())
                return;
            VideoFrameWithPacket firstVideoFrame;
            firstVideoFrame = videoQueue.get(0);
            // In case of video copy we need to reorder these frames back howe
            // they were in the stream. We use the original frame number for
            // this.
            if (videoCopy) {
                for (VideoFrameWithPacket videoFrame : videoQueue) {
                    if (videoFrame.getPacket().getFrameNo() < firstVideoFrame.getPacket().getFrameNo())
                        firstVideoFrame = videoFrame;
                }
            }

            // If we have .2s of leading audio, output it with the the current
            // video frame
            int aqSize = audioQueue.size();
            for (int af = 0; af < aqSize; af++) {
                AudioFrameWithPacket audioFrame = audioQueue.get(0);
                if (audioFrame.getPacket().getPtsD() >= firstVideoFrame.getPacket().getPtsD() + .2)
                    break;
                audioQueue.remove(0);

                if (audioCopy && (sink instanceof PacketSink)) {
                    ((PacketSink) sink).outputAudioPacket(audioFrame.getPacket(), audioCodecMeta);
                } else {
                    sink.outputAudioFrame(audioFrame);
                }
            }
            videoQueue.remove(firstVideoFrame);
            if (videoCopy && (sink instanceof PacketSink)) {
                ((PacketSink) sink).outputVideoPacket(firstVideoFrame.getPacket(), videoCodecMeta);
            } else {
                // Filtering the pixels
                LoanerPicture frame = firstVideoFrame.getFrame();
                for (Filter filter : filters) {
                    LoanerPicture old = frame;
                    frame = filter.filter(frame.getPicture(), pixelStore);
                    // Filters that don't change the original picture will
                    // return null
                    if (frame == null) {
                        frame = old;
                    } else {
                        pixelStore.putBack(old);
                    }
                }
                sink.outputVideoFrame(new VideoFrameWithPacket(firstVideoFrame.getPacket(), frame));
                pixelStore.putBack(frame);
            }
        }

        public void finalFlushQueues() throws IOException {
            VideoFrameWithPacket lastVideoFrame = null;
            for (VideoFrameWithPacket videoFrame : videoQueue) {
                if (lastVideoFrame == null || videoFrame.getPacket().getPtsD() >= lastVideoFrame.getPacket().getPtsD())
                    lastVideoFrame = videoFrame;
            }
            if (lastVideoFrame != null) {
                for (AudioFrameWithPacket audioFrame : audioQueue) {
                    // Don't output audio when there's no video any more
                    if (audioFrame.getPacket().getPtsD() > lastVideoFrame.getPacket().getPtsD())
                        break;
                    if (audioCopy && (sink instanceof PacketSink)) {
                        ((PacketSink) sink).outputAudioPacket(audioFrame.getPacket(), audioCodecMeta);
                    } else {
                        sink.outputAudioFrame(audioFrame);
                    }
                }
                for (VideoFrameWithPacket videoFrame : videoQueue) {
                    if (videoFrame != null) {
                        if (videoCopy && (sink instanceof PacketSink)) {
                            ((PacketSink) sink).outputVideoPacket(videoFrame.getPacket(), videoCodecMeta);
                        } else {
                            sink.outputVideoFrame(videoFrame);
                            pixelStore.putBack(videoFrame.getFrame());
                        }
                    }
                }
            }
        }

        public void addVideoPacket(VideoFrameWithPacket videoFrame, VideoCodecMeta meta) {
            if (videoFrame.getFrame() != null)
                pixelStore.retake(videoFrame.getFrame());
            this.videoQueue.add(videoFrame);
            this.videoCodecMeta = meta;
            if (this.filters == null)
                this.filters = initColorTransform(videoCodecMeta.getColor(), extraFilters, sink);
        }

        public void addAudioPacket(AudioFrameWithPacket videoFrame, AudioCodecMeta meta) {
            this.audioQueue.add(videoFrame);
            this.audioCodecMeta = meta;
        }

        public boolean needsVideoFrame() {
            if (videoQueue.size() <= 0)
                return true;
            if (videoCopy && videoQueue.size() < REORDER_LENGTH)
                return true;
            return haveEnoughAudio();
        }

        private boolean haveEnoughAudio() {
            VideoFrameWithPacket firstVideoFrame = videoQueue.get(0);
            for (AudioFrameWithPacket audioFrame : audioQueue) {
                if (audioFrame.getPacket().getPtsD() >= firstVideoFrame.getPacket().getPtsD() + AUDIO_LEADING_TIME) {
                    return true;
                }
            }
            return false;
        }
    }

    public void transcode() throws IOException {
        PixelStore pixelStore = new PixelStoreImpl();

        List<Stream>[] videoStreams = new List[sources.length];
        List<Stream>[] audioStreams = new List[sources.length];
        boolean[] decodeVideo = new boolean[sources.length];
        boolean[] decodeAudio = new boolean[sources.length];
        boolean[] finishedVideo = new boolean[sources.length];
        boolean[] finishedAudio = new boolean[sources.length];
        Stream[] allStreams = new Stream[sinks.length];
        int[] videoFramesRead = new int[sources.length];

        for (int s = 0; s < sources.length; s++) {
            videoStreams[s] = new ArrayList<Stream>();
            audioStreams[s] = new ArrayList<Stream>();
        }

        for (int i = 0; i < sinks.length; i++)
            sinks[i].init();

        for (int i = 0; i < sources.length; i++) {
            sources[i].init(pixelStore);
            sources[i].seekFrames(seekFrames[i]);
        }

        for (int s = 0; s < sinks.length; s++) {
            Stream stream = new Stream(sinks[s], videoMappings[s].copy, audioMappings[s].copy, extraFilters[s],
                    pixelStore);
            allStreams[s] = stream;
            videoStreams[videoMappings[s].source].add(stream);
            audioStreams[audioMappings[s].source].add(stream);
            if (!videoMappings[s].copy)
                decodeVideo[videoMappings[s].source] = true;
            if (!audioMappings[s].copy)
                decodeAudio[audioMappings[s].source] = true;
        }

        try {
            while (true) {
                // Read video and audio packet from each source and add it to
                // the appropriate queues
                for (int s = 0; s < sources.length; s++) {
                    Source source = sources[s];

                    // See if we need to read a video frame, if out of the sinks
                    // still doesn't have enough audio don't read the next video
                    // frame just yet
                    boolean needsVideoFrame = !finishedVideo[s];
                    for (Stream stream : videoStreams[s]) {
                        needsVideoFrame &= stream.needsVideoFrame();
                    }

                    if (needsVideoFrame) {
                        // Read the next video frame and give it to all the
                        // streams that need it
                        VideoFrameWithPacket nextVideoFrame;
                        if (videoFramesRead[s] >= maxFrames[s]) {
                            nextVideoFrame = null;
                            finishedVideo[s] = true;
                        } else if (decodeVideo[s] || !(source instanceof PacketSource)) {
                            nextVideoFrame = source.getNextVideoFrame();
                            if (nextVideoFrame == null) {
                                finishedVideo[s] = true;
                            } else {
                                ++videoFramesRead[s];
                                printLegend((int) nextVideoFrame.getPacket().getFrameNo(), 0,
                                        nextVideoFrame.getPacket());
                            }
                        } else {
                            Packet packet = ((PacketSource) source).inputVideoPacket();
                            if (packet == null) {
                                finishedVideo[s] = true;
                            } else {
                                ++videoFramesRead[s];
                            }
                            nextVideoFrame = new VideoFrameWithPacket(packet, null);
                        }

                        // The video source is empty, clear all video streams
                        // feeding from it, also locate and clean all these
                        // streams from audio feed
                        if (finishedVideo[s]) {
                            for (Stream stream : videoStreams[s]) {
                                for (int ss = 0; ss < audioStreams.length; ss++) {
                                    audioStreams[ss].remove(stream);
                                }
                            }
                            videoStreams[s].clear();
                        }

                        if (nextVideoFrame != null) {
                            for (Stream stream : videoStreams[s]) {
                                stream.addVideoPacket(nextVideoFrame, source.getVideoCodecMeta());
                            }
                            // De-reference the frame because it should be
                            // already in the queues by now, if nobody needs it
                            // it will just go away with this.
                            if (nextVideoFrame.getFrame() != null)
                                pixelStore.putBack(nextVideoFrame.getFrame());
                        }
                    }

                    // If no streams in need for this audio don't bother reading
                    if (!audioStreams[s].isEmpty()) {
                        // Read the next audio frame (or packet) and give it to
                        // all
                        // the streams that want it
                        AudioFrameWithPacket nextAudioFrame;
                        if (decodeAudio[s] || !(source instanceof PacketSource)) {
                            nextAudioFrame = source.getNextAudioFrame();
                            if (nextAudioFrame == null)
                                finishedAudio[s] = true;
                        } else {
                            Packet packet = ((PacketSource) source).inputAudioPacket();
                            if (packet == null)
                                finishedAudio[s] = true;
                            nextAudioFrame = new AudioFrameWithPacket(null, packet);
                        }
                        if (nextAudioFrame != null) {
                            for (Stream stream : audioStreams[s]) {
                                stream.addAudioPacket(nextAudioFrame, source.getAudioCodecMeta());
                            }
                        }
                    } else {
                        finishedAudio[s] = true;
                    }
                }

                // See if we can produce any output with the new frames just
                // read
                for (int s = 0; s < allStreams.length; s++) {
                    allStreams[s].tryFlushQueues();
                }

                // Are we drained on all sources
                boolean allFinished = true;
                for (int s = 0; s < sources.length; s++) {
                    allFinished &= finishedVideo[s] & finishedAudio[s];
                }

                if (allFinished)
                    break;
            }
            // Finally flush everything that remains
            for (int s = 0; s < allStreams.length; s++) {
                allStreams[s].finalFlushQueues();
            }
        } finally {
            for (int i = 0; i < sources.length; i++)
                sources[0].finish();
            for (int i = 0; i < sinks.length; i++)
                sinks[i].finish();
        }
    }

    private void printLegend(int frameNo, int maxFrames, Packet inVideoPacket) {
        if (frameNo % 100 == 0)
            System.out.print(String.format("[%6d]\r", frameNo));
    }

    public static class TranscoderBuilder {

        private List<Source> source = new ArrayList<Source>();
        private List<Sink> sink = new ArrayList<Sink>();
        private List<List<Filter>> filters = new ArrayList<List<Filter>>();
        private IntArrayList seekFrames = new IntArrayList(20);
        private IntArrayList maxFrames = new IntArrayList(20);
        private List<Mapping> videoMappings = new ArrayList<Mapping>();
        private List<Mapping> audioMappings = new ArrayList<Mapping>();

        public TranscoderBuilder() {
        }

        public TranscoderBuilder addFilter(int sink, Filter filter) {
            this.filters.get(sink).add(filter);
            return this;
        }

        public TranscoderBuilder setSeekFrames(int source, int seekFrames) {
            this.seekFrames.set(source, seekFrames);
            return this;
        }

        public TranscoderBuilder setMaxFrames(int source, int maxFrames) {
            this.maxFrames.set(source, maxFrames);
            return this;
        }

        public TranscoderBuilder addSource(Source source) {
            this.source.add(source);
            this.seekFrames.add(0);
            this.maxFrames.add(Integer.MAX_VALUE);
            return this;
        }

        public TranscoderBuilder addSink(Sink sink) {
            this.sink.add(sink);
            videoMappings.add(new Mapping(0, false));
            audioMappings.add(new Mapping(0, false));
            filters.add(new ArrayList<Filter>());
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
                    videoMappings.toArray(new Mapping[] {}), audioMappings.toArray(new Mapping[] {}),
                    filters.toArray(new List[0]), seekFrames.toArray(), maxFrames.toArray());
        }
    }

    public static TranscoderBuilder newTranscoder() {
        return new TranscoderBuilder();
    }
}