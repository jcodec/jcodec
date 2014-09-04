package org.jcodec.movtool.streaming;

import java.io.Closeable;
import java.io.IOException;

import org.jcodec.containers.mp4.boxes.SampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie track
 * 
 * @author The JCodec project
 * 
 */
public interface VirtualTrack extends Closeable {

    VirtualPacket nextPacket() throws IOException;
    
    SampleEntry getSampleEntry();
    
    VirtualEdit[] getEdits();
    
    int getPreferredTimescale();

    public static class VirtualEdit {
        private double in;
        private double duration;

        public VirtualEdit(double in, double duration) {
            this.in = in;
            this.duration = duration;
        }

        public double getIn() {
            return in;
        }

        public double getDuration() {
            return duration;
        }
    }
}
