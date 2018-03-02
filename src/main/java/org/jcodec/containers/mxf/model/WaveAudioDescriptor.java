package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import java.lang.System;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class WaveAudioDescriptor extends GenericSoundEssenceDescriptor {

    private short blockAlign;
    private byte sequenceOffset;
    private int avgBps;
    private UL channelAssignment;
    private int peakEnvelopeVersion;
    private int peakEnvelopeFormat;
    private int pointsPerPeakValue;
    private int peakEnvelopeBlockSize;
    private int peakChannels;
    private int peakFrames;
    private ByteBuffer peakOfPeaksPosition;
    private ByteBuffer peakEnvelopeTimestamp;
    private ByteBuffer peakEnvelopeData;

    public WaveAudioDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3d0a:
                blockAlign = _bb.getShort();
                break;
            case 0x3d0b:
                sequenceOffset = _bb.get();
                break;
            case 0x3d09:
                avgBps = _bb.getInt();
                break;
            case 0x3d32:
                channelAssignment = UL.read(_bb);
                break;
            case 0x3d29:
                peakEnvelopeVersion = _bb.getInt();
                break;
            case 0x3d2a:
                peakEnvelopeFormat = _bb.getInt();
                break;
            case 0x3d2b:
                pointsPerPeakValue = _bb.getInt();
                break;
            case 0x3d2c:
                peakEnvelopeBlockSize = _bb.getInt();
                break;
            case 0x3d2d:
                peakChannels = _bb.getInt();
                break;
            case 0x3d2e:
                peakFrames = _bb.getInt();
                break;
            case 0x3d2f:
                peakOfPeaksPosition = _bb;
                break;
            case 0x3d30:
                peakEnvelopeTimestamp = _bb;
                break;
            case 0x3d31:
                peakEnvelopeData = _bb;
                break;

            default:
                System.out.println(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public short getBlockAlign() {
        return blockAlign;
    }

    public byte getSequenceOffset() {
        return sequenceOffset;
    }

    public int getAvgBps() {
        return avgBps;
    }

    public UL getChannelAssignment() {
        return channelAssignment;
    }

    public int getPeakEnvelopeVersion() {
        return peakEnvelopeVersion;
    }

    public int getPeakEnvelopeFormat() {
        return peakEnvelopeFormat;
    }

    public int getPointsPerPeakValue() {
        return pointsPerPeakValue;
    }

    public int getPeakEnvelopeBlockSize() {
        return peakEnvelopeBlockSize;
    }

    public int getPeakChannels() {
        return peakChannels;
    }

    public int getPeakFrames() {
        return peakFrames;
    }

    public ByteBuffer getPeakOfPeaksPosition() {
        return peakOfPeaksPosition;
    }

    public ByteBuffer getPeakEnvelopeTimestamp() {
        return peakEnvelopeTimestamp;
    }

    public ByteBuffer getPeakEnvelopeData() {
        return peakEnvelopeData;
    }
}