package org.jcodec.movtool.streaming.tracks;

import static java.util.Arrays.binarySearch;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGUtil.nextSegment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.ConstantRateControl;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

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

    public static final int TARGET_RATE = 1024;
    private int frameSize;
    protected VirtualTrack src;
    private SampleEntry se;
    private ThreadLocal<MPEGToAVCTranscoder> transcoders = new ThreadLocal<MPEGToAVCTranscoder>();
    int mbW;
    int mbH;
    int scaleFactor;
    int thumbWidth;
    int thumbHeight;
    private GOP gop;
    private GOP prevGop;
    private VirtualPacket nextPacket;

    protected void checkFourCC(VirtualTrack srcTrack) {
        String fourcc = srcTrack.getSampleEntry().getFourcc();
        if (!"m2v1".equals(fourcc))
            throw new IllegalArgumentException("Input track is not ProRes");
    }

    protected int selectScaleFactor(Size frameDim) {
        return frameDim.getWidth() >= 960 ? 2 : (frameDim.getWidth() > 480 ? 1 : 0);
    }

    public Mpeg2AVCTrack(VirtualTrack src) throws IOException {
        checkFourCC(src);
        this.src = src;
        ConstantRateControl rc = new ConstantRateControl(TARGET_RATE);
        H264Encoder encoder = new H264Encoder(rc);

        nextPacket = src.nextPacket();
        Size frameDim = MPEGDecoder.getSize(nextPacket.getData());

        scaleFactor = selectScaleFactor(frameDim);
        thumbWidth = frameDim.getWidth() >> scaleFactor;
        thumbHeight = (frameDim.getHeight() >> scaleFactor) & ~1;

        mbW = (thumbWidth + 15) >> 4;
        mbH = (thumbHeight + 15) >> 4;

        se = H264Utils.createMOVSampleEntry(encoder.initSPS(new Size(thumbWidth, thumbHeight)), encoder.initPPS());
        PixelAspectExt pasp = Box.findFirst(src.getSampleEntry(), PixelAspectExt.class, "pasp");
        if (pasp != null)
            se.add(pasp);

        frameSize = rc.calcFrameSize(mbW * mbH);
        frameSize += frameSize >> 4;
    }

    @Override
    public SampleEntry getSampleEntry() {
        return se;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (nextPacket == null)
            return null;

        if (nextPacket.isKeyframe()) {
            prevGop = gop;
            gop = new GOP(nextPacket.getFrameNo(), prevGop);
            if (prevGop != null)
                prevGop.setNextGop(gop);
        }

        VirtualPacket ret = gop.addPacket(nextPacket);

        nextPacket = src.nextPacket();

        return ret;
    }

    private class GOP {
        private List<VirtualPacket> packets = new ArrayList<VirtualPacket>();
        private ByteBuffer[] data;
        private int frameNo;
        private GOP nextGop;
        private GOP prevGop;
        private List<ByteBuffer> leadingB;

        public GOP(int frameNo, GOP prevGop) {
            this.frameNo = frameNo;
            this.prevGop = prevGop;
        }

        public void setNextGop(GOP gop) {
            this.nextGop = gop;
        }

        public VirtualPacket addPacket(VirtualPacket pkt) {
            packets.add(pkt);
            return new TranscodePacket(pkt, this, packets.size() - 1);
        }

        private synchronized void transcode() throws IOException {
            if (data != null)
                return;

            // System.out.println("Transcoding GOP: " + frameNo);
            data = new ByteBuffer[packets.size()];
            for (int tr = 0; tr < 2; tr++) {
                try {
                    MPEGToAVCTranscoder t = transcoders.get();
                    if (t == null) {
                        t = createTranscoder(scaleFactor);
                        transcoders.set(t);
                    }
                    carryLeadingBOver();

                    double[] pts = collectPts(packets);

                    for (int numRefs = 0, i = 0; i < packets.size(); i++) {
                        VirtualPacket pkt = packets.get(i);
                        ByteBuffer pktData = pkt.getData();
                        int picType = getPicType(pktData.duplicate());
                        if (picType != PictureHeader.BiPredictiveCoded) {
                            ++numRefs;
                        } else if (numRefs < 2) {
                            continue;
                        }
                        ByteBuffer buf = ByteBuffer.allocate(frameSize);
                        data[i] = t.transcodeFrame(pktData, buf, i == 0, binarySearch(pts, pkt.getPts()));
                    }

                    if (nextGop != null) {
                        nextGop.leadingB = new ArrayList<ByteBuffer>();

                        pts = collectPts(nextGop.packets);

                        for (int numRefs = 0, i = 0; i < nextGop.packets.size(); i++) {
                            VirtualPacket pkt = nextGop.packets.get(i);
                            ByteBuffer pktData = pkt.getData();

                            int picType = getPicType(pktData.duplicate());
                            if (picType != PictureHeader.BiPredictiveCoded)
                                ++numRefs;
                            if (numRefs >= 2)
                                break;

                            ByteBuffer buf = ByteBuffer.allocate(frameSize);
                            nextGop.leadingB
                                    .add(t.transcodeFrame(pktData, buf, i == 0, binarySearch(pts, pkt.getPts())));
                        }
                    }
                    break;
                } catch (Throwable t) {
                    System.out.println("Error transcoding gop: " + t.getMessage() + ", retrying.");
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

    protected MPEGToAVCTranscoder createTranscoder(int scaleFactor) {
        return new MPEGToAVCTranscoder(scaleFactor);
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

    private class TranscodePacket extends VirtualPacketWrapper {
        private GOP gop;
        private int index;

        public TranscodePacket(VirtualPacket nextPacket, GOP gop, int index) {
            super(nextPacket);
            this.gop = gop;
            this.index = index;
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