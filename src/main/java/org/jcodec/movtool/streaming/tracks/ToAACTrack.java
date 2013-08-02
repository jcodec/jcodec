package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;

import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ToAACTrack implements VirtualTrack {

    @Override
    public VirtualPacket nextPacket() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CodecMeta getCodecMeta() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualEdit[] getEdits() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

}
