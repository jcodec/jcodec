package org.jcodec.api.specific;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.api.FrameGrab.MediaInfo;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.flv.FLVDemuxer;
import org.jcodec.containers.flv.FLVPacket;
import org.jcodec.containers.flv.FLVPacket.AvcVideoTagHeader;
import org.jcodec.containers.flv.FLVPacket.TagHeader;
import org.jcodec.containers.flv.FLVTrackDemuxer.FLVDemuxerTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Frame grabber adapter for AVC in FLV combination
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class AVCFLVAdaptor extends CodecContainerAdaptor {
    private static final int MAX_ANALYSE_FRAMES = 1000;
    private ThreadLocal<int[][]> buffers = new ThreadLocal<int[][]>();
    private Size size;
    private H264Decoder decoder;
    private AvcCBox avcC;

    public AVCFLVAdaptor(SeekableDemuxerTrack track) throws IOException {
        super(track);
        lookupSPSPPS();
    }

    @Override
    public Picture nextFrame() throws IOException {
        for (Packet pkt = track().nextFrame(); pkt != null; pkt = track().nextFrame()) {
            Picture pic = decodePacket(pkt);
            if (pic != null)
                return pic;
        }
        // This is the end of stream
        return null;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return new MediaInfo(size);
    }

    @Override
    public void seek(double second) throws IOException {
        track().seek(second);
        for (Packet pkt : gobackToKeyFrame()) {
            decodePacket(pkt);
        }
    }

    @Override
    public void gotoFrame(int frameNumber) throws IOException {
        track().gotoFrame(frameNumber);
        for (Packet pkt : gobackToKeyFrame()) {
            decodePacket(pkt);
        }
    }

    @Override
    public void seekToKeyFrame(double second) throws IOException {
        track().seek(second);
        gobackToKeyFrame();
    }

    @Override
    public void gotoToKeyFrame(int frameNumber) throws IOException {
        track().gotoFrame(frameNumber);
        gobackToKeyFrame();
    }

    private void lookupSPSPPS() throws IOException {
        int frames = 0;
        for (Packet pkt = track().nextFrame(); pkt != null && frames < MAX_ANALYSE_FRAMES; pkt = track().nextFrame(), ++frames) {
            TagHeader tagHeader = ((FLVPacket) pkt).getTagHeader();
            if (!(tagHeader instanceof AvcVideoTagHeader)) {
                continue;
            }
            AvcVideoTagHeader avcTag = (AvcVideoTagHeader) tagHeader;
            if (avcTag.getAvcPacketType() == 0) {
                ByteBuffer frameData = pkt.getData().duplicate();
                FLVDemuxer.parseVideoTagHeader(frameData);
                updateDecoder(frameData);
                break;
            }
        }
        track().gotoFrame(0);
        if (frames == MAX_ANALYSE_FRAMES) {
            throw new IOException("Couldn't find SPS/PPS for " + frames + " frames.");
        }
    }

    private Picture decodePacket(Packet pkt) {
        TagHeader tagHeader = ((FLVPacket) pkt).getTagHeader();
        if (!(tagHeader instanceof AvcVideoTagHeader)) {
            return null;
        }
        AvcVideoTagHeader avcTag = (AvcVideoTagHeader) tagHeader;
        ByteBuffer frameData = pkt.getData().duplicate();
        FLVDemuxer.parseVideoTagHeader(frameData);
        if (avcTag.getAvcPacketType() == 0) {
            updateDecoder(frameData);
            return null;
        } else {
            return decoder.decodeFrame(H264Utils.splitMOVPacket(frameData, avcC), getBuffer());
        }
    }

    private void updateDecoder(ByteBuffer data2) {
        avcC = H264Utils.parseAVCC(data2);
        size = getSize(avcC);
        decoder = new H264Decoder();
        ((H264Decoder) decoder).addSps(avcC.getSpsList());
        ((H264Decoder) decoder).addPps(avcC.getPpsList());
    }

    public static Size getSize(AvcCBox avcC) {
        int w = Integer.MIN_VALUE, h = Integer.MIN_VALUE;
        for (SeqParameterSet sps : H264Utils.readSPS(avcC.getSpsList())) {
            int ww = sps.pic_width_in_mbs_minus1 + 1;
            if (ww > w)
                w = ww;
            int hh = H264Utils.getPicHeightInMbs(sps);
            if (hh > h)
                h = hh;
        }

        return new Size(w << 4, h << 4);
    }

    private int[][] getBuffer() {
        int[][] buf = buffers.get();
        if (buf == null || buf[0].length != size.getWidth() * size.getHeight()) {
            buf = Picture.create(size.getWidth(), size.getHeight(), ColorSpace.YUV444).getData();
            buffers.set(buf);
        }
        return buf;
    }

    private List<Packet> gobackToKeyFrame() throws IOException {
        List<Packet> result = new LinkedList<Packet>();
        Packet cur = ((FLVDemuxerTrack) track()).pickFrame();
        if (cur.isKeyFrame())
            return result;

        for (Packet pkt = ((FLVDemuxerTrack) track()).prevFrame(); pkt != null; pkt = ((FLVDemuxerTrack) track())
                .prevFrame()) {
            result.add(0, pkt);
            if (pkt.isKeyFrame())
                return result;
        }
        // No keyframe was found
        throw new IOException("No keyframe found before the seek point");
    }
}