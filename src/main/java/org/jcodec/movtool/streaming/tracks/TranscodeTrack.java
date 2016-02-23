package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

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
    private ThreadLocal<Transcoder> transcoders;
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
        this.transcoders = new ThreadLocal<Transcoder>();
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

        se = new VideoCodecMeta("avc1", codecPrivate, size, pasp);

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
        return new TranscodePacket(this, nextPacket);
    }

    private static class TranscodePacket extends VirtualPacketWrapper {
        private TranscodeTrack track;

		public TranscodePacket(TranscodeTrack track, VirtualPacket nextPacket) {
            super(nextPacket);
			this.track = track;
        }

        @Override
        public int getDataLen() {
            return track.frameSize;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            Transcoder t = track.transcoders.get();
            if (t == null) {
                t = new Transcoder(track);
                track.transcoders.set(t);
            }
            ByteBuffer buf = ByteBuffer.allocate(track.frameSize);
            ByteBuffer data = src.getData();
            return t.transcodeFrame(data, buf);
        }
    }

   static class Transcoder {
        private VideoDecoder decoder;
        private VideoEncoder[] encoder;
        private Picture pic0;
        private Picture pic1;
        private Transform transform;
		private TranscodeTrack track;

        public Transcoder(TranscodeTrack track) {
            this.encoder = new VideoEncoder[3];
            this.track = track;
			this.decoder = track.getDecoder(track.scaleFactor);
            this.encoder[0] = track.getEncoder(TARGET_RATE);
            this.encoder[1] = track.getEncoder((int) (TARGET_RATE * 0.9));
            this.encoder[2] = track.getEncoder((int) (TARGET_RATE * 0.8));
            pic0 = Picture.create(track.mbW << 4, track.mbH << 4, ColorSpace.YUV444);
        }

        public ByteBuffer transcodeFrame(ByteBuffer src, ByteBuffer dst) {
            Picture decoded = decoder.decodeFrame(src, pic0.getData());
            if (pic1 == null) {
                pic1 = Picture.create(decoded.getWidth(), decoded.getHeight(), ColorSpace.YUV420);
                transform = ColorUtil.getTransform(decoded.getColor(), ColorSpace.YUV420);
            }
            transform.transform(decoded, pic1);
            pic1.setCrop(new Rect(0, 0, track.thumbWidth, track.thumbHeight));
            // Rate control, reinforcement mechanism
            for (int i = 0; i < encoder.length; i++) {
                try {
                    dst.clear();
                    ByteBuffer out = encoder[i].encodeFrame(pic1, dst);
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