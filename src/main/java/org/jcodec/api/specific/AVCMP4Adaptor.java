package org.jcodec.api.specific;

import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;

import org.jcodec.api.FrameGrab.MediaInfo;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
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
 * High level frame grabber helper.
 * 
 * @author The JCodec project
 * 
 */
public class AVCMP4Adaptor implements ContainerAdaptor {

    private H264Decoder decoder;
    private SampleEntry[] ses;
    private AvcCBox avcCBox;
    private int curENo;
    private Size size;

    public AVCMP4Adaptor(SampleEntry[] ses) {
        this.ses = ses;
        this.curENo = -1;

        calcBufferSize();
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

    public AVCMP4Adaptor(AbstractMP4DemuxerTrack vt) {
        this(((AbstractMP4DemuxerTrack) vt).getSampleEntries());
    }

    public Picture decodeFrame(Packet packet, int[][] data) {
        updateState(packet);

        Picture pic = decoder.decodeFrame(H264Utils.splitMOVPacket(packet.getData(), avcCBox), data);
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
            decoder.addSps(avcCBox.getSpsList());
            decoder.addPps(avcCBox.getPpsList());
        }
    }

    @Override
    public boolean canSeek(Packet pkt) {
        updateState(pkt);
        return H264Utils.idrSlice(splitMOVPacket(pkt.getData(), avcCBox));
    }

    @Override
    public int[][] allocatePicture() {
        return Picture.create(size.getWidth(), size.getHeight(), ColorSpace.YUV444).getData();
    }

	@Override
	public MediaInfo getMediaInfo() {
		return new MediaInfo(size);
	}
}