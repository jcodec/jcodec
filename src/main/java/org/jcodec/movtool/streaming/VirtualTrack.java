package org.jcodec.movtool.streaming;

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
public interface VirtualTrack {

    VirtualPacket nextPacket() throws IOException;

    SampleEntry getSampleEntry();

    void close();
}
