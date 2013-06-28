package org.jcodec.api.specific;

import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
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

    public AVCMP4Adaptor(SampleEntry[] ses) {
        this.ses = ses;
        this.curENo = -1;
    }

    public AVCMP4Adaptor(AbstractMP4DemuxerTrack vt) {
        this(((AbstractMP4DemuxerTrack) vt).getSampleEntries());
    }

    public Picture decodeFrame(Packet packet, int[][] data) {
        updateState(packet);

        Picture pic = ((H264Decoder) decoder).decodeFrame(H264Utils.splitMOVPacket(packet.getData(), avcCBox), data);
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
            avcCBox = new AvcCBox();
            avcCBox.parse(Box.findFirst(ses[curENo], LeafBox.class, "avcC").getData());
            decoder = new H264Decoder();
            ((H264Decoder) decoder).addSps(avcCBox.getSpsList());
            ((H264Decoder) decoder).addPps(avcCBox.getPpsList());
        }
    }

    @Override
    public boolean canSeek(Packet pkt) {
        updateState(pkt);
        return H264Utils.idrSlice(splitMOVPacket(pkt.getData(), avcCBox));
    }
}