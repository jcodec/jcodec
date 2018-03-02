package org.jcodec.containers.mps;

import java.nio.ByteBuffer;

public class PESPacket {
    public ByteBuffer data;
    public long pts;
    public int streamId;
    public int length;
    public long pos;
    public long dts;

    public PESPacket(ByteBuffer data, long pts, int streamId, int length, long pos, long dts) {
        this.data = data;
        this.pts = pts;
        this.streamId = streamId;
        this.length = length;
        this.pos = pos;
        this.dts = dts;
    }
}