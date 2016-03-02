package org.jcodec.movtool.streaming;
import java.lang.IllegalStateException;
import java.lang.System;


import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie track
 * 
 * @author The JCodec project
 * 
 */
public interface VirtualTrack {

    VirtualPacket nextPacket() throws IOException;
    
    CodecMeta getCodecMeta();
    
    VirtualEdit[] getEdits();
    
    int getPreferredTimescale();
    
    void close() throws IOException;
    
    public static class VirtualEdit {
        private double _in;
        private double duration;

        public VirtualEdit(double _in, double duration) {
            this._in = _in;
            this.duration = duration;
        }

        public double getIn() {
            return _in;
        }

        public double getDuration() {
            return duration;
        }
    }
}
