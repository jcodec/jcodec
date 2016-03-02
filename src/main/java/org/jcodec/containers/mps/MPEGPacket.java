package org.jcodec.containers.mps;

import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;

import java.nio.ByteBuffer;

public class MPEGPacket extends Packet {
    private long offset;
    private ByteBuffer seq;
    private int gop;
    private int timecode;

    public MPEGPacket(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean keyFrame,
            TapeTimecode tapeTimecode) {
        super(data, pts, timescale, duration, frameNo, keyFrame, tapeTimecode, 0);
    }

    public long getOffset() {
        return offset;
    }

    public ByteBuffer getSeq() {
        return seq;
    }

    public int getGOP() {
        return gop;
    }

    public int getTimecode() {
        return timecode;
    }
}