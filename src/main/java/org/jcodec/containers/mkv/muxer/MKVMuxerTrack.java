package org.jcodec.containers.mkv.muxer;

import static org.jcodec.containers.mkv.MKVType.createByType;
import static org.jcodec.containers.mkv.boxes.MkvBlock.anyFrame;

import java.io.IOException;

import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mkv.CuesFactory;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MKVMuxerTrack implements MuxerTrack {

    public static enum MKVMuxerTrackType {
        VIDEO, AUDIO
    };

    public MKVMuxerTrackType type;
    public VideoCodecMeta videoMeta;
    public String codecId;
    public int trackNo;
    public long trackStart;
    public EbmlMaster firstCluster;
    public MkvBlock lastFrame;
    private int frameDuration;
    private Rational frameRate;
    private MkvBlock clusterHeadFrame;
    private EbmlMaster currentCluster;
    final SeekableByteChannel os;
    final CuesFactory cf;

    public MKVMuxerTrack(SeekableByteChannel os, CuesFactory cf) {
        this.type = MKVMuxerTrackType.VIDEO;
        this.os = os;
        this.cf = cf;
    }

    static final int DEFAULT_TIMESCALE = 1000000000; // NANOSECOND

    static final int NANOSECONDS_IN_A_MILISECOND = 1000000;
    static final int MULTIPLIER = DEFAULT_TIMESCALE / NANOSECONDS_IN_A_MILISECOND;

    public int getTimescale() {
        return NANOSECONDS_IN_A_MILISECOND;
    }

    public Rational getFrameRate() {
        return frameRate;
    }

    @Override
    public void addFrame(Packet outPacket) {
        MkvBlock frame = anyFrame(trackNo, 0, outPacket.getData(), outPacket.isKeyFrame());
        if (frameRate == null || frameRate.den != outPacket.duration) {
            frameRate = new Rational((int) outPacket.duration, outPacket.timescale);
        }
        frame.absoluteTimecode = outPacket.getPts();
        lastFrame = frame;
        // Creates one cluster per each keyframe. Before staring a new cluster we will
        // write the previous to the disk.
        if (outPacket.isKeyFrame()) {
            muxCurrentCluster();
            currentCluster = singleBlockedCluster(frame);
            if (firstCluster == null) {
                firstCluster = currentCluster;
            }
        } else {
            if (currentCluster == null) {
                throw new RuntimeException("The first frame must be a keyframe in an MKV file");
            }
            frame.timecode = (int) (frame.absoluteTimecode - clusterHeadFrame.absoluteTimecode);
            currentCluster.add(frame);
        }
    }

    public long getTrackNo() {
        return trackNo;
    }

    private void muxCurrentCluster() {
        if (currentCluster != null) {
            try {
                currentCluster.mux(os);
                cf.add(CuesFactory.CuePointMock.make(currentCluster));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void finish() {
        muxCurrentCluster();
    }

    private EbmlMaster singleBlockedCluster(MkvBlock aBlock) {
        EbmlMaster mkvCluster = createByType(MKVType.Cluster);
        MKVMuxer.createLong(mkvCluster, MKVType.Timecode, aBlock.absoluteTimecode - aBlock.timecode);
        mkvCluster.add(aBlock);
        clusterHeadFrame = aBlock;
        return mkvCluster;
    }

}
