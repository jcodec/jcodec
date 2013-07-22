package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.vpx.VP8FixedRateControl;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Generic transcode track
 * 
 * @author The JCodec project
 * 
 */
public abstract class TranscodeTrack implements VirtualTrack {

    private static final int TARGET_RATE = 1024;
    private int frameSize;
    private VirtualTrack proresTrack;
    private SampleEntry se;
    private ThreadLocal<Transcoder> transcoders = new ThreadLocal<Transcoder>();
    private int mbW;
    private int mbH;
    private int scaleFactor;
    private int thumbWidth;
    private int thumbHeight;

    public TranscodeTrack(VirtualTrack proresTrack, Size frameDim) {
        checkFourCC(proresTrack);
        this.proresTrack = proresTrack;
        H264FixedRateControl rc = new H264FixedRateControl(TARGET_RATE);
        H264Encoder encoder = new H264Encoder(rc);

        scaleFactor = frameDim.getWidth() >= 960 ? 2 : 1;
        thumbWidth = frameDim.getWidth() >> scaleFactor;
        thumbHeight = (frameDim.getHeight() >> scaleFactor) & ~1;

        mbW = (thumbWidth + 15) >> 4;
        mbH = (thumbHeight + 15) >> 4;

        se = H264Utils.createMOVSampleEntry(encoder.initSPS(new Size(thumbWidth, thumbHeight)), encoder.initPPS());
        PixelAspectExt pasp = Box.findFirst(proresTrack.getSampleEntry(), PixelAspectExt.class, "pasp");
        if (pasp != null)
            se.add(pasp);

        frameSize = rc.calcFrameSize(mbW * mbH);
        frameSize += frameSize >> 4;
    }

    private void checkFourCC(VirtualTrack proresTrack) {
        String fourcc = proresTrack.getSampleEntry().getFourcc();
        if ("ap4h".equals(fourcc))
            return;
        for (Profile profile : EnumSet.allOf(ProresEncoder.Profile.class)) {
            if (profile.fourcc.equals(fourcc))
                return;
        }
        throw new IllegalArgumentException("Input track is not ProRes");
    }

    @Override
    public SampleEntry getSampleEntry() {
        return se;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        VirtualPacket nextPacket = proresTrack.nextPacket();
        if (nextPacket == null)
            return null;
        return new TranscodePacket(nextPacket);
    }

    private class TranscodePacket extends VirtualPacketWrapper {
        public TranscodePacket(VirtualPacket nextPacket) {
            super(nextPacket);
        }

        @Override
        public int getDataLen() {
            return frameSize;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            Transcoder t = transcoders.get();
            if (t == null) {
                t = new Transcoder();
                transcoders.set(t);
            }
            ByteBuffer buf = ByteBuffer.allocate(frameSize);
            ByteBuffer data = src.getData();
            return t.transcodeFrame(data, buf);
        }
    }

    protected abstract VideoDecoder getDecoder(int scaleFactor);

    protected abstract VideoEncoder getEncoder(int rate);

    class Transcoder {
        private VideoDecoder decoder;
        private VideoEncoder[] encoder = new VideoEncoder[3];
        private Picture pic0;
        private Picture pic1;
        private Transform transform;

        public Transcoder() {
            this.decoder = getDecoder(scaleFactor);
            this.encoder[0] = getEncoder(TARGET_RATE);
            this.encoder[1] = getEncoder((int) (TARGET_RATE * 0.9));
            this.encoder[2] = getEncoder((int) (TARGET_RATE * 0.8));
            pic0 = Picture.create(mbW << 4, mbH << 4, ColorSpace.YUV444);
        }

        public ByteBuffer transcodeFrame(ByteBuffer src, ByteBuffer dst) throws IOException {
            Picture decoded = decoder.decodeFrame(src, pic0.getData());
            if (pic1 == null) {
                pic1 = Picture.create(decoded.getWidth(), decoded.getHeight(), ColorSpace.YUV420);
                transform = ColorUtil.getTransform(decoded.getColor(), ColorSpace.YUV420);
            }
            transform.transform(decoded, pic1);
            pic1.setCrop(new Rect(0, 0, thumbWidth, thumbHeight));
            // Rate control, reinforcement mechanism
            for (int i = 0; i < encoder.length; i++) {
                try {
                    encoder[i].encodeFrame(dst, pic1);
                    break;
                } catch (BufferOverflowException ex) {
                    System.out.println("Abandon frame!!!");
                }
            }

            H264Utils.encodeMOVPacket(dst, null, null);
            return dst;
        }
    }

    @Override
    public void close() throws IOException {
        proresTrack.close();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return proresTrack.getEdits();
    }

    @Override
    public int getPreferredTimescale() {
        return proresTrack.getPreferredTimescale();
    }

    public static class Prores2AVCTrack extends TranscodeTrack {

        public Prores2AVCTrack(VirtualTrack proresTrack, Size frameDim) {
            super(proresTrack, frameDim);
        }

        @Override
        protected VideoDecoder getDecoder(int scaleFactor) {
            return scaleFactor == 2 ? new ProresToThumb2x2() : new ProresToThumb4x4();
        }

        @Override
        protected VideoEncoder getEncoder(int rate) {
            return new H264Encoder(new H264FixedRateControl(rate));
        }
    }

    public static class Prores2VP8Track extends TranscodeTrack {

        public Prores2VP8Track(VirtualTrack proresTrack, Size frameDim) {
            super(proresTrack, frameDim);
        }

        @Override
        protected VideoDecoder getDecoder(int scaleFactor) {
            return scaleFactor == 2 ? new ProresToThumb2x2() : new ProresToThumb4x4();
        }

        @Override
        protected VideoEncoder getEncoder(int rate) {
            return new VP8Encoder(new VP8FixedRateControl(rate));
        }
    }
}