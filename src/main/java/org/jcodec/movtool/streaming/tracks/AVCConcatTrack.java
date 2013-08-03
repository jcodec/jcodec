package org.jcodec.movtool.streaming.tracks;

import org.jcodec.containers.mp4.boxes.SampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Concats 2 AVC tracks, special logic to merge codec private is applied
 * 
 * @author The JCodec project
 * 
 */
public class AVCConcatTrack extends ConcatTrack {

    public AVCConcatTrack(RealTrack[] tracks) {
        super(tracks);
    }
    
    @Override
    public SampleEntry getSampleEntry() {
        return tracks[0].getSampleEntry();
    }
}
