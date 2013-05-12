package org.jcodec.common;

import java.io.IOException;

import org.jcodec.common.model.Packet;

public interface DemuxerTrack {

    Packet getFrames(int n) throws IOException;

    boolean gotoFrame(long i);

    long getCurFrame();

    void seek(double second);

}
