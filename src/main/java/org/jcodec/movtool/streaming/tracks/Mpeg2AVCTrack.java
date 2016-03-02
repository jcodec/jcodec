package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;
import java.lang.ThreadLocal;
import java.lang.IllegalArgumentException;

import static java.util.Arrays.binarySearch;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGUtil.nextSegment;
import static org.jcodec.movtool.streaming.tracks.MPEGToAVCTranscoder.createTranscoder;
import static org.jcodec.movtool.streaming.tracks.Transcode2AVCTrack.createCodecMeta;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.mpeg12.MPEGConst;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Size;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie track that transcodes ProRes to AVC on the fly.
 * 
 * @author The JCodec project
 * 
 */
public class Mpeg2AVCTrack implements VirtualTrack {

    private final int frameSize;
    protected VirtualTrack src;
    private CodecMeta se;
    private ThreadLocal<MPEGToAVCTranscoder> transcoders;
    int mbW;
    int mbH;
    int scaleFactor;
    int thumbWidth;
    int thumbHeight;
    private GOP gop;
    private GOP prevGop;
    private VirtualPacket _nextPacket;

    protected void checkFourCC(VirtualTrack srcTrack) {
        String fourcc = srcTrack.getCodecMeta().getFourcc();
        if (!"m2v1".equals(fourcc))
            throw new IllegalArgumentException("Input track is not ProRes");
    }

    protected int selectScaleFactor(Size frameDim) {
        return frameDim.getWidth() >= 960 ? 2 : (frameDim.getWidth() > 480 ? 1 : 0);
    }

    public Mpeg2AVCTrack(VirtualTrack src) throws IOException {
        this.transcoders = new ThreadLocal<MPEGToAVCTranscoder>();

        checkFourCC(src);
        this.src = src;
        H264FixedRateControl rc = new H264FixedRateControl(MPEGToAVCTranscoder.TARGET_RATE);
        H264Encoder encoder = new H264Encoder(rc);

        _nextPacket = src.nextPacket();
        Size frameDim = MPEGDecoder.getSize(_nextPacket.getData());

        scaleFactor = selectScaleFactor(frameDim);
        thumbWidth = frameDim.getWidth() >> scaleFactor;
        thumbHeight = (frameDim.getHeight() >> scaleFactor) & ~1;

        mbW = (thumbWidth + 15) >> 4;
        mbH = (thumbHeight + 15) >> 4;

        se = createCodecMeta(src, encoder, thumbWidth, thumbHeight);

        int _frameSize = rc.calcFrameSize(mbW * mbH);
        _frameSize += _frameSize >> 4;
        this.frameSize = _frameSize;
    }

    @Override
    public CodecMeta getCodecMeta() {
        return se;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (_nextPacket == null)
            return null;

        if (_nextPacket.isKeyframe()) {
            prevGop = gop;
            gop = new GOP(this, _nextPacket.getFrameNo(), prevGop);
            if (prevGop != null)
                prevGop.setNextGop(gop);
        }

        VirtualPacket ret = gop.addPacket(_nextPacket);

        _nextPacket = src.nextPacket();

        return ret;
    }

    private static class GOP {
        private List<VirtualPacket> packets;
        private ByteBuffer[] data;
        private int frameNo;
        private GOP nextGop;
        private GOP prevGop;
        private List<ByteBuffer> leadingB;
		private Mpeg2AVCTrack track;

        public GOP(Mpeg2AVCTrack track, int frameNo, GOP prevGop) {
            this.packets = new ArrayList<VirtualPacket>();
            this.track = track;
			this.frameNo = frameNo;
            this.prevGop = prevGop;
        }

        public void setNextGop(GOP gop) {
            this.nextGop = gop;
        }

        public VirtualPacket addPacket(VirtualPacket pkt) {
            packets.add(pkt);
            return new TranscodePacket(pkt, this, packets.size() - 1, track.frameSize);
        }

        private synchronized void transcode() throws IOException {
            if (data != null)
                return;

            // System.out.println("Transcoding GOP: " + frameNo);
            data = new ByteBuffer[packets.size()];
            for (int tr = 0; tr < 2; tr++) {
                try {
                    MPEGToAVCTranscoder t = track.transcoders.get();
                    if (t == null) {
                        t = createTranscoder(track.scaleFactor);
                        track.transcoders.set(t);
                    }
                    carryLeadingBOver();

                    double[] pts = collectPts(packets);

                    for (int numRefs = 0, i = 0; i < packets.size(); i++) {
                        VirtualPacket pkt = packets.get(i);
                        ByteBuffer pktData = pkt.getData();
                        int picType = getPicType(pktData.duplicate());
                        if (picType != MPEGConst.BiPredictiveCoded) {
                            ++numRefs;
                        } else if (numRefs < 2) {
                            continue;
                        }
                        ByteBuffer buf = ByteBuffer.allocate(track.frameSize);
                        data[i] = t.transcodeFrame(pktData, buf, i == 0, binarySearch(pts, pkt.getPts()));
                    }

                    if (nextGop != null) {
                        nextGop.leadingB = new ArrayList<ByteBuffer>();

                        pts = collectPts(nextGop.packets);

                        for (int numRefs = 0, i = 0; i < nextGop.packets.size(); i++) {
                            VirtualPacket pkt = nextGop.packets.get(i);
                            ByteBuffer pktData = pkt.getData();

                            int picType = getPicType(pktData.duplicate());
                            if (picType != MPEGConst.BiPredictiveCoded)
                                ++numRefs;
                            if (numRefs >= 2)
                                break;

                            ByteBuffer buf = ByteBuffer.allocate(track.frameSize);
                            nextGop.leadingB
                                    .add(t.transcodeFrame(pktData, buf, i == 0, binarySearch(pts, pkt.getPts())));
                        }
                    }
                    break;
                } catch (Throwable t) {
                    Logger.error("Error transcoding gop: " + t.getMessage() + ", retrying.");
                }
            }
        }

        private double[] collectPts(List<VirtualPacket> packets2) {
            double[] pts = new double[packets2.size()];
            for (int i = 0; i < pts.length; i++)
                pts[i] = packets2.get(i).getPts();
            Arrays.sort(pts);
            return pts;
        }

        private synchronized void carryLeadingBOver() {
            if (leadingB != null) {
                for (int i = 0; i < leadingB.size(); i++) {
                    data[i] = leadingB.get(i);
                }
            }
        }

        public ByteBuffer getData(int i) throws IOException {
            transcode();
            if (data[i] == null && prevGop != null) {
                prevGop.transcode();
                carryLeadingBOver();
            }
            return data[i];
        }
    }

    public static int getPicType(ByteBuffer buf) {
        ByteBuffer segment;

        while ((segment = nextSegment(buf)) != null) {
            int code = segment.getInt() & 0xff;
            if (code == PICTURE_START_CODE) {
                PictureHeader ph = PictureHeader.read(segment);
                return ph.picture_coding_type;
            }
        }
        return -1;
    }

    private static class TranscodePacket extends VirtualPacketWrapper {
        private GOP gop;
        private int index;
		private int frameSize;

        public TranscodePacket(VirtualPacket nextPacket, GOP gop, int index, int frameSize) {
            super(nextPacket);
            this.gop = gop;
            this.index = index;
			this.frameSize = frameSize;
        }

        @Override
        public int getDataLen() {
            return frameSize;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            return gop.getData(index);
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