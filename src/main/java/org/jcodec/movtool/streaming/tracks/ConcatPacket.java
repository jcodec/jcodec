package org.jcodec.movtool.streaming.tracks;
import js.lang.IllegalStateException;
import js.lang.System;


import org.jcodec.movtool.streaming.VirtualPacket;

import js.io.IOException;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ConcatPacket implements VirtualPacket {
    private VirtualPacket packet;
    private double ptsOffset;
    private int fnOffset;

    public ConcatPacket(VirtualPacket packet, double ptsOffset, int fnOffset) {
        this.packet = packet;
        this.ptsOffset = ptsOffset;
        this.fnOffset = fnOffset;
    }

    @Override
    public ByteBuffer getData() throws IOException {
        return packet.getData();
    }

    @Override
    public int getDataLen() throws IOException {
        return packet.getDataLen();
    }

    @Override
    public double getPts() {
        return ptsOffset + packet.getPts();
    }

    @Override
    public double getDuration() {
        return packet.getDuration();
    }

    @Override
    public boolean isKeyframe() {
        return packet.isKeyframe();
    }

    @Override
    public int getFrameNo() {
        return fnOffset + packet.getFrameNo();
    }

}