package org.jcodec.containers.mkv.muxer;

import static org.jcodec.containers.mkv.boxes.MkvBlock.keyFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.model.Size;
import org.jcodec.containers.mkv.boxes.MkvBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MKVMuxerTrack {

    public static enum MKVMuxerTrackType {VIDEO };
    
    public MKVMuxerTrackType type;
    public Size frameDimentions;
    public String codecId;
    public int trackNo;
    private int frameDuration;
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

    public void addSampleEntry(ByteBuffer frameData, int pts) {
        MkvBlock frame = keyFrame(trackNo, 0, frameData);
        frame.absoluteTimecode = pts - 1;
        trackBlocks.add(frame);
    }

    public long getTrackNo() {
        return trackNo;
    }

}
