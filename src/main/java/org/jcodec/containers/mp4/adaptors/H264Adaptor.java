package org.jcodec.containers.mp4.adaptors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.h264.AccessUnit;
import org.jcodec.codecs.h264.H264Demuxer;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Adapts MP4 container to a role of H264 demuxer
 * 
 * 
 * @author Jay Codec
 * 
 */
public class H264Adaptor implements H264Demuxer {
    private Map<Integer, SeqParameterSet> spsSet = new HashMap<Integer, SeqParameterSet>();
    private Map<Integer, PictureParameterSet> ppsSet = new HashMap<Integer, PictureParameterSet>();
    private MP4Demuxer.FramesTrack track;

    public H264Adaptor(MP4Demuxer.FramesTrack track) {
        this.track = track;

        readStreamParams(track);
    }

    public AccessUnit nextAcceessUnit() throws IOException {
        Packet pkt = track.getFrames(1);

        if (pkt == null)
            return null;

        return new AUFromContainerSample(pkt.getData().toArray());
    }

    public PictureParameterSet getPPS(int id) {
        return ppsSet.get(id);
    }

    public SeqParameterSet getSPS(int id) {
        return spsSet.get(id);
    }

    private void readStreamParams(MP4Demuxer.DemuxerTrack track) {
        VideoSampleEntry sampleEntry = (VideoSampleEntry) track.getSampleEntries()[0];

        AvcCBox avcC = NodeBox.findFirst(sampleEntry, AvcCBox.class, AvcCBox.fourcc());

        for (PictureParameterSet pps : avcC.getPpsList()) {
            ppsSet.put(pps.pic_parameter_set_id, pps);
        }

        for (SeqParameterSet sps : avcC.getSpsList()) {
            spsSet.put(sps.seq_parameter_set_id, sps);
        }
    }
}