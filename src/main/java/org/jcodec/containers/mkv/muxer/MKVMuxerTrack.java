package org.jcodec.containers.mkv.muxer;
import static org.jcodec.containers.mkv.boxes.MkvBlock.keyFrame;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mkv.boxes.MkvBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MKVMuxerTrack implements MuxerTrack {

    public static enum MKVMuxerTrackType {VIDEO };
    
    public MKVMuxerTrackType type;
    public VideoCodecMeta videoMeta;
    public String codecId;
    public int trackNo;
    private int frameDuration;
    private Rational frameRate;
    List<MkvBlock> trackBlocks;
    
    public MKVMuxerTrack() {
        this.trackBlocks = new ArrayList<MkvBlock>();
        this.type = MKVMuxerTrackType.VIDEO;
    }
    
    static final int DEFAULT_TIMESCALE = 1000000000; //NANOSECOND
    
    static final int NANOSECONDS_IN_A_MILISECOND = 1000000; 
    static final int MULTIPLIER = DEFAULT_TIMESCALE/NANOSECONDS_IN_A_MILISECOND;
    
    public int getTimescale(){
        return NANOSECONDS_IN_A_MILISECOND;
    }
    
    public Rational getFrameRate() {
    	return frameRate;
    }

    @Override
    public void addFrame(Packet outPacket) {
        MkvBlock frame = keyFrame(trackNo, 0, outPacket.getData());
        if(frameRate==null||frameRate.den!=outPacket.duration) {
        	frameRate=new Rational((int)outPacket.duration,outPacket.timescale);
        }
        frame.absoluteTimecode = outPacket.getPts();
        trackBlocks.add(frame);
    }

    public long getTrackNo() {
        return trackNo;
    }
}
