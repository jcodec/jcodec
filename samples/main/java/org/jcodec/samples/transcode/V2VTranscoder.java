package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.samples.transcode.filters.ColorTransformFilter;

/**
 * Generic transcode loop.
 * 
 * @author Stanislav Vitvitskiy
 */
public abstract class V2VTranscoder implements Transcoder {
    private static final String FLAG_SEEK_FRAMES = "seek-frames";
    private static final String FLAG_MAX_FRAMES = "max-frames";
    private static final int REORDER_BUFFER_SIZE = 5;

    /**
     * Filters the decoded image before it gets to encoder.
     * 
     * @author stan
     */
    public static interface Filter {
        Picture8Bit filter(Picture8Bit picture, PixelStore store);
    }

    public static interface PixelStore {
        Picture8Bit getPicture(int width, int height, ColorSpace color);

        void putBack(Picture8Bit frame);
    }

    public static class PixelStoreImpl implements PixelStore {
        private List<Picture8Bit> buffers = new ArrayList<Picture8Bit>();

        @Override
        public Picture8Bit getPicture(int width, int height, ColorSpace color) {
            for (Picture8Bit picture8Bit : buffers) {
                if (picture8Bit.getWidth() == width && picture8Bit.getHeight() == height
                        && picture8Bit.getColor() == color) {
                    buffers.remove(picture8Bit);
                    return picture8Bit;
                }
            }
            return Picture8Bit.create(width, height, color);
        }

        @Override
        public void putBack(Picture8Bit frame) {
            frame.setCrop(null);
            buffers.add(frame);
        }
    }

    protected abstract GenericTranscoder getTranscoder(Cmd cmd, Profile profile);

    public static abstract class GenericTranscoder {
        private ThreadLocal<Picture8Bit> pixelBufferStore = new ThreadLocal<Picture8Bit>();
        private ThreadLocal<ByteBuffer> bufferStore = new ThreadLocal<ByteBuffer>();
        protected Cmd cmd;
        protected Profile profile;

        public GenericTranscoder(Cmd cmd, Profile profile) {
            this.cmd = cmd;
            this.profile = profile;
        }

        protected abstract void initDecode(SeekableByteChannel source) throws IOException;

        protected abstract void initEncode(SeekableByteChannel sink) throws IOException;

        protected abstract void finishEncode() throws IOException;

        protected Picture8Bit createPixelBuffer(ColorSpace yuv444, ByteBuffer firstFrame) {
            return null;
        }

        protected ColorSpace getEncoderColorspace() {
            return null;
        }

        protected abstract Packet inputVideoPacket() throws IOException;

        protected abstract void outputVideoPacket(Packet packet) throws IOException;

        protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
            return null;
        }

        protected EncodedFrame encodeVideo(Picture8Bit frame, ByteBuffer _out) {
            return null;
        }

        protected abstract boolean haveAudio();

        protected abstract Packet inputAudioPacket() throws IOException;

        protected abstract void outputAudioPacket(Packet audioPkt) throws IOException;

        protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
            return null;
        }

        protected ByteBuffer encodeAudio(ByteBuffer wrap) {
            return null;
        }

        protected boolean seek(int frame) throws IOException {
            return false;
        }

        protected int getBufferSize(Picture8Bit frame) {
            return 0;
        }

        protected List<Filter> getFilters() {
            return new ArrayList<Filter>();
        }

        protected boolean audioCodecCopy() {
            return false;
        }

        protected boolean videoCodecCopy() {
            return false;
        }

        private class FrameWithPacket implements Comparable<FrameWithPacket> {
            private Packet packet;
            private Picture8Bit frame;

            public FrameWithPacket(Packet inFrame, Picture8Bit dec2) {
                this.packet = inFrame;
                this.frame = dec2;
            }

            @Override
            public int compareTo(FrameWithPacket arg) {
                if (arg == null)
                    return -1;
                else {
                    long pts1 = packet.getPts();
                    long pts2 = arg.packet.getPts();
                    return pts1 > pts2 ? 1 : (pts1 == pts2 ? 0 : -1);
                }
            }
        }

        public void transcode() throws IOException {
            SeekableByteChannel sink = null;
            SeekableByteChannel source = null;
            List<FrameWithPacket> reorderBuffer = new ArrayList<FrameWithPacket>();
            try {
                source = readableFileChannel(cmd.getArg(0));
                sink = writableFileChannel(cmd.getArg(1));

                initDecode(source);
                initEncode(sink);

                Integer sf = cmd.getIntegerFlag(FLAG_SEEK_FRAMES);
                int skipFrames = 0;
                if (sf != null) {
                    if (!seek(sf)) {
                        Logger.warn("Unable to seek, will have to skip.");
                        skipFrames = sf;
                    }
                }

                int maxFrames = cmd.getIntegerFlagD(FLAG_MAX_FRAMES, Integer.MAX_VALUE - skipFrames) + skipFrames;

                Packet inVideoPacket;
                boolean framesDecoded = false;
                List<Filter> filters = getFilters();
                PixelStore pixelsStore = new PixelStoreImpl();
                filters.add(0, new ColorTransformFilter(getEncoderColorspace()));
                for (int frameNo = 0; (inVideoPacket = inputVideoPacket()) != null && frameNo <= maxFrames; frameNo++) {
                    if (skipFrames > 0) {
                        skipFrames--;
                        continue;
                    }
                    if (!inVideoPacket.isKeyFrame() && !framesDecoded) {
                        continue;
                    }
                    framesDecoded = true;
                    if (haveAudio()) {
                        double endPts = inVideoPacket.getPtsD() + 0.2;
                        outputAudioPacketsTo(endPts);
                    }

                    if (!videoCodecCopy()) {
                        Picture8Bit pixelBuffer = pixelBufferStore.get();
                        if (pixelBuffer == null) {
                            pixelBuffer = createPixelBuffer(ColorSpace.YUV444, inVideoPacket.getData());
                            pixelBufferStore.set(pixelBuffer);
                        }
                        Picture8Bit decodedFrame = decodeVideo(inVideoPacket.getData(), pixelBuffer);
                        if(decodedFrame == null)
                            continue;
                        printLegend(frameNo, maxFrames, inVideoPacket);
                        for (Filter filter : filters) {
                            decodedFrame = filter.filter(decodedFrame, pixelsStore);
                        }
                        if (reorderBuffer.size() > REORDER_BUFFER_SIZE) {
                            outFrames(reorderBuffer, pixelsStore, 1);
                        }
                        reorderBuffer.add(new FrameWithPacket(inVideoPacket, decodedFrame));
                    } else {
                        outputVideoPacket(inVideoPacket);
                    }
                }

                if (reorderBuffer.size() > 0) {
                    outFrames(reorderBuffer, pixelsStore, reorderBuffer.size());
                }
                // Remaining audio packets
                outputAudioPacketsTo(Double.MAX_VALUE);
                finishEncode();
            } finally {
                IOUtils.closeQuietly(sink);
                IOUtils.closeQuietly(source);
            }
        }

        private void outputAudioPacketsTo(double endPts) throws IOException {
            Packet audioPkt;
            do {
                audioPkt = inputAudioPacket();
                if (audioPkt == null)
                    break;
                if (!audioCodecCopy()) {
                    ByteBuffer decodedAudio = decodeAudio(audioPkt.getData());
                    outputAudioPacket(Packet.createPacketWithData(audioPkt, encodeAudio(decodedAudio)));
                } else {
                    outputAudioPacket(audioPkt);
                }
            } while (audioPkt.getPtsD() < endPts);
        }

        private void printLegend(int frameNo, int maxFrames, Packet inVideoPacket) {
            if (frameNo % 100 == 0)
                System.out.print(String.format("[%6d]\r", frameNo));
        }

        private void outFrames(List<FrameWithPacket> frames, PixelStore pixelStore, int nFrames) throws IOException {
            Collections.sort(frames);
            for (int i = 0; i < nFrames; i++) {
                FrameWithPacket frame = frames.remove(0);
                ByteBuffer buffer = bufferStore.get();
                int bufferSize = getBufferSize(frame.frame);
                if (buffer == null || bufferSize < buffer.capacity()) {
                    buffer = ByteBuffer.allocate(bufferSize);
                    bufferStore.set(buffer);
                }
                buffer.clear();
                EncodedFrame enc = encodeVideo(frame.frame, buffer);
                pixelStore.putBack(frame.frame);
                Packet outputVideoPacket = Packet.createPacketWithData(frame.packet, NIOUtils.clone(enc.getData()));
                outputVideoPacket.setKeyFrame(enc.isKeyFrame());
                outputVideoPacket(outputVideoPacket);
            }
        }
    }

    protected void additionalFlags(Map<String, String> flags) {
    }

    @Override
    public void printHelp(PrintStream err) {
        Map<String, String> flags = new HashMap<String, String>();

        flags.put(FLAG_SEEK_FRAMES, "Seek frames");
        flags.put(FLAG_MAX_FRAMES, "Max frames");

        additionalFlags(flags);

        MainUtils.printHelpVarArgs(flags, "in file", "pattern");
    }

    @Override
    public void transcode(Cmd cmd, Profile profile) throws IOException {
        GenericTranscoder transcoder = getTranscoder(cmd, profile);
        transcoder.transcode();
    }
}