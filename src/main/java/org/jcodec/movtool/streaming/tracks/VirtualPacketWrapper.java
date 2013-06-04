package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.movtool.streaming.VirtualPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Wraps virtual packet for easy proxy creation
 * 
 * @author The JCodec project
 * 
 */
public class VirtualPacketWrapper implements VirtualPacket {
    protected VirtualPacket src;

    public VirtualPacketWrapper(VirtualPacket src) {
        this.src = src;
    }

    @Override
    public ByteBuffer getData() throws IOException {
        return src.getData();
    }

    @Override
    public int getDataLen() {
        return src.getDataLen();
    }

    @Override
    public double getPts() {
        return src.getPts();
    }

    @Override
    public double getDuration() {
        return src.getDuration();
    }

    @Override
    public boolean isKeyframe() {
        return src.isKeyframe();
    }

    @Override
    public int getFrameNo() {
        return src.getFrameNo();
    }

}
