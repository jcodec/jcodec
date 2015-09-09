package org.jcodec.api.specific;

import java.io.IOException;

import org.jcodec.api.FrameGrab.MediaInfo;
import org.jcodec.api.JCodecException;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Frame grabber adapter for AVC in MP4 combination
 * 
 * @author The JCodec project
 * 
 */
public class AVCMP4Adaptor extends CodecContainerAdaptor {
    private ThreadLocal<int[][]> buffers = new ThreadLocal<int[][]>();

    private H264Decoder decoder;
    private SampleEntry[] ses;
    private AvcCBox avcCBox;
    private int curENo;
    private Size size;

    public AVCMP4Adaptor(SeekableDemuxerTrack track) throws IOException, JCodecException {
        super(track);
        this.ses = ((AbstractMP4DemuxerTrack) track()).getSampleEntries();
        this.curENo = -1;

        calcBufferSize();
        decodeLeadingFrames();
    }

    private void calcBufferSize() {
        int w = Integer.MIN_VALUE, h = Integer.MIN_VALUE;
        for (SampleEntry se : ses) {
            if ("avc1".equals(se.getFourcc())) {
                AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) se);
                for (SeqParameterSet sps : H264Utils.readSPS(avcC.getSpsList())) {
                    int ww = sps.pic_width_in_mbs_minus1 + 1;
                    if (ww > w)
                        w = ww;
                    int hh = H264Utils.getPicHeightInMbs(sps);
                    if (hh > h)
                        h = hh;
                }
            }
        }

        size = new Size(w << 4, h << 4);
    }

    public Picture nextFrame() throws IOException {
        for (;;) {
            Packet packet = track().nextFrame();
            if (packet == null)
                return null;

            Picture frame = decodeFrame(packet);
            if (frame != null)
                return frame;
        }
    }
    
    public Picture decodeFrame(Packet packet) {
        updateState(packet);

        Picture pic = decoder.decodeFrame(H264Utils.splitMOVPacket(packet.getData(), avcCBox), getBuffer());
        PixelAspectExt pasp = Box.findFirst(ses[curENo], PixelAspectExt.class, "pasp");

        if (pasp != null) {
            // TODO: transform
        }

        return pic;
    }

    private void updateState(Packet packet) {
        int eNo = ((MP4Packet) packet).getEntryNo();
        if (eNo != curENo) {
            curENo = eNo;
            avcCBox = H264Utils.parseAVCC((VideoSampleEntry) ses[curENo]);
            decoder = new H264Decoder();
            ((H264Decoder) decoder).addSps(avcCBox.getSpsList());
            ((H264Decoder) decoder).addPps(avcCBox.getPpsList());
        }
    }

    @Override
    public MediaInfo getMediaInfo() {
        return new MediaInfo(size);
    }

    private int[][] getBuffer() {
        int[][] buf = buffers.get();
        if (buf == null) {
            buf = Picture.create(size.getWidth(), size.getHeight(), ColorSpace.YUV444).getData();
            buffers.set(buf);
        }
        return buf;
    }

    @Override
    public void seek(double second) throws IOException {
        track().seek(second);
        decodeLeadingFrames();
    }

    @Override
    public void gotoFrame(int frameNumber) throws IOException {
        track().gotoFrame(frameNumber);
        decodeLeadingFrames();
    }

    @Override
    public void seekToKeyFrame(double second) throws IOException {
        track().seek(second);
        goToPrevKeyframe();
    }

    @Override
    public void gotoToKeyFrame(int frameNumber) throws IOException {
        track().gotoFrame(frameNumber);
        goToPrevKeyframe();
    }

    private void goToPrevKeyframe() throws IOException {
        track().gotoFrame(detectKeyFrame((int) track().getCurFrame()));
    }

    private int detectKeyFrame(int start) throws IOException {
        int[] seekFrames = track().getMeta().getSeekFrames();
        if (seekFrames == null)
            return start;
        int prev = seekFrames[0];
        for (int i = 1; i < seekFrames.length; i++) {
            if (seekFrames[i] > start)
                break;
            prev = seekFrames[i];
        }
        return prev;
    }

    private void decodeLeadingFrames() throws IOException {
        SeekableDemuxerTrack sdt = track();

        int curFrame = (int) sdt.getCurFrame();
        int keyFrame = detectKeyFrame(curFrame);
        if (keyFrame != curFrame)
            sdt.gotoFrame(keyFrame);

        Packet frame = sdt.nextFrame();

        while (frame.getFrameNo() < curFrame) {
            decodeFrame(frame);
            frame = sdt.nextFrame();
        }
        sdt.gotoFrame(curFrame);
    }

//    private CodecContainerAdaptor detectDecoder(SeekableDemuxerTrack videoTrack, Packet frame) throws JCodecException {
//        if (videoTrack instanceof AbstractMP4DemuxerTrack) {
//            SampleEntry se = ((AbstractMP4DemuxerTrack) videoTrack).getSampleEntries()[((MP4Packet) frame).getEntryNo()];
//            VideoDecoder byFourcc = byFourcc(se.getHeader().getFourcc());
//            if (byFourcc instanceof H264Decoder)
//                return new AVCMP4Adaptor(((AbstractMP4DemuxerTrack) videoTrack).getSampleEntries());
//        } else if (videoTrack instanceof FLVDemuxerTrack) {
//            FLVDemuxerTrack demuxerTrack = (FLVDemuxerTrack) videoTrack;
//            Codec codec = demuxerTrack.getCodec();
//            if (codec == Codec.H264)
//                return new AVCFLVAdaptor();
//        }
//
//        throw new UnsupportedFormatException("Codec is not supported");
//    }
}