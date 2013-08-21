package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.ConstantRateControl;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
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
import org.jcodec.scale.Yuv422pToYuv420p;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie track that transcodes ProRes to AVC on the fly.
 * 
 * @author The JCodec project
 * 
 */
public class Prores2AVCTrack implements VirtualTrack {
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

    public Prores2AVCTrack(VirtualTrack proresTrack, Size frameDim) {
        checkFourCC(proresTrack);
        this.proresTrack = proresTrack;
        ConstantRateControl rc = new ConstantRateControl(TARGET_RATE);
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

    class Transcoder {
        private ProresDecoder decoder;
        private H264Encoder encoder;
        private Picture pic0;
        private Picture pic1;
        private Transform transform;
        private ConstantRateControl rc;

        public Transcoder() {
            rc = new ConstantRateControl(TARGET_RATE);
            this.decoder = scaleFactor == 2 ? new ProresToThumb2x2() : new ProresToThumb4x4();
            this.encoder = new H264Encoder(rc);
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
            int rate = TARGET_RATE;
            do {
                try {
                    encoder.encodeFrame(dst, pic1);
                    break;
                } catch (BufferOverflowException ex) {
                    System.out.println("Abandon frame!!!");
                    rate -= 10;
                    rc.setRate(rate);
                }
            } while (rate > 10);
            rc.setRate(TARGET_RATE);

            H264Utils.encodeMOVPacket(dst);
            
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
}