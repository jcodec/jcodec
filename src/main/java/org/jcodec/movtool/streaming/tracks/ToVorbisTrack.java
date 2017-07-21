package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;

import org.jcodec.common.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ToVorbisTrack implements VirtualTrack {

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
