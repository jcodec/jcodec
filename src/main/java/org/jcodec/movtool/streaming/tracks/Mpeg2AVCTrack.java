package org.jcodec.movtool.streaming.tracks;

import static java.util.Arrays.binarySearch;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGUtil.nextSegment;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.ConstantRateControl;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.Mpeg2Thumb2x2;
import org.jcodec.codecs.mpeg12.Mpeg2Thumb4x4;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.VideoDecoder;
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
 * Virtual movie track that transcodes ProRes to AVC on the fly.
 * 
 * @author The JCodec project
 * 
 */
public class Mpeg2AVCTrack implements VirtualTrack {

    private static final int TARGET_RATE = 1024;
    private int frameSize;
    protected VirtualTrack src;
    private SampleEntry se;
    private ThreadLocal<Transcoder> transcoders = new ThreadLocal<Transcoder>();
    private int mbW;
    private int mbH;
    private int scaleFactor;
    private int thumbWidth;
    private int thumbHeight;
    private GOP gop;
    private GOP prevGop;

    protected void checkFourCC(VirtualTrack srcTrack) {
        String fourcc = srcTrack.getSampleEntry().getFourcc();
        if (!"m2v1".equals(fourcc))
            throw new IllegalArgumentException("Input track is not ProRes");
    }

    protected int selectScaleFactor(Size frameDim) {
        return frameDim.getWidth() >= 960 ? 2 : (frameDim.getWidth() > 480 ? 1 : 0);
    }

    protected VideoDecoder getDecoder(int scaleFactor) {
        switch (scaleFactor) {
        case 2:
            return new Mpeg2Thumb2x2();
        case 1:
            return new Mpeg2Thumb4x4();
        case 0:
            return new MPEGDecoder();
        default:
            throw new IllegalArgumentException("Unsupported scale factor: " + scaleFactor);
        }
    }

    public Mpeg2AVCTrack(VirtualTrack src, Size frameDim) {
        checkFourCC(src);
        this.src = src;
        ConstantRateControl rc = new ConstantRateControl(TARGET_RATE);
        H264Encoder encoder = new H264Encoder(rc);

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
        VirtualPacket nextPacket = src.nextPacket();

        if (nextPacket == null)
            return null;

        if (nextPacket.isKeyframe()) {
            prevGop = gop;
            gop = new GOP(nextPacket.getFrameNo(), prevGop);
            if (prevGop != null)
                prevGop.setNextGop(gop);
        }

        return gop.addPacket(nextPacket);
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

//            System.out.println("Transcoding GOP: " + frameNo);

            data = new ByteBuffer[packets.size()];
            Transcoder t = transcoders.get();
            if (t == null) {
                t = new Transcoder();
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
                    nextGop.leadingB.add(t.transcodeFrame(pktData, buf, i == 0, binarySearch(pts, pkt.getPts())));
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

    class Transcoder {
        private VideoDecoder decoder;
        private H264Encoder encoder;
        private Picture pic0;
        private Picture pic1;
        private Transform transform;
        private ConstantRateControl rc;

        public Transcoder() {
            rc = new ConstantRateControl(TARGET_RATE);
            this.decoder = getDecoder(scaleFactor);
            this.encoder = new H264Encoder(rc);
            pic0 = Picture.create(mbW << 4, (mbH + 1) << 4, ColorSpace.YUV444);
        }

        public ByteBuffer transcodeFrame(ByteBuffer src, ByteBuffer dst, boolean iframe, int poc) throws IOException {
            if (src == null)
                return null;
            Picture decoded = decoder.decodeFrame(src, pic0.getData());
            if (pic1 == null) {
                pic1 = Picture.create(decoded.getWidth(), decoded.getHeight(), encoder.getSupportedColorSpaces()[0]);
                transform = ColorUtil.getTransform(decoded.getColor(), encoder.getSupportedColorSpaces()[0]);
            }
            transform.transform(decoded, pic1);
            pic1.setCrop(new Rect(0, 0, thumbWidth, thumbHeight));
            int rate = TARGET_RATE;
            do {
                try {
                    encoder.encodeFrame(pic1, dst, iframe, poc);
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