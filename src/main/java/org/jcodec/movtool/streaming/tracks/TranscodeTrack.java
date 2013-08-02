package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VideoCodecMeta;
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
    private VirtualTrack src;
    private CodecMeta se;
    private ThreadLocal<Transcoder> transcoders = new ThreadLocal<Transcoder>();
    private int mbW;
    private int mbH;
    private int scaleFactor;
    private int thumbWidth;
    private int thumbHeight;

    protected abstract int getFrameSize(int mbCount, int rate);

    protected abstract VideoDecoder getDecoder(int scaleFactor);

    protected abstract VideoEncoder getEncoder(int rate);

    protected abstract void getCodecPrivate(ByteBuffer buf, Size size);

    public TranscodeTrack(VirtualTrack proresTrack, Size frameDim) {
        this.src = proresTrack;

        scaleFactor = frameDim.getWidth() >= 960 ? 2 : 1;
        thumbWidth = frameDim.getWidth() >> scaleFactor;
        thumbHeight = (frameDim.getHeight() >> scaleFactor) & ~1;

        mbW = (thumbWidth + 15) >> 4;
        mbH = (thumbHeight + 15) >> 4;

        Size size = new Size(thumbWidth, thumbHeight);
        Rational pasp = ((VideoCodecMeta) proresTrack.getCodecMeta()).getPasp();

        ByteBuffer codecPrivate = ByteBuffer.allocate(1024);
        getCodecPrivate(codecPrivate, size);

        se = new VideoCodecMeta("avc1", size, pasp, codecPrivate);

        frameSize = getFrameSize(mbW * mbH, TARGET_RATE);
        frameSize += frameSize >> 4;
    }

    @Override
    public CodecMeta getCodecMeta() {
        return se;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        VirtualPacket nextPacket = src.nextPacket();
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

        public ByteBuffer transcodeFrame(ByteBuffer src, ByteBuffer dst) {
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
                    dst.clear();
                    ByteBuffer out = encoder[i].encodeFrame(dst, pic1);
                    break;
                } catch (BufferOverflowException ex) {
                    System.out.println("Abandon frame!!!");
                }
            }
            
            return dst;
        }
    }

    @Override
    public void close() throws IOException {
        src.close();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return src.getEdits();
    }

    @Override
    public int getPreferredTimescale() {
        return src.getPreferredTimescale();
    }
}