package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

import js.nio.ByteBuffer;
import js.util.Map;
import js.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEG2VideoDescriptor extends CDCIEssenceDescriptor {
    private byte singleSequence;
    private byte constantBFrames;
    private byte codedContentType;
    private byte lowDelay;
    private byte closedGOP;
    private byte identicalGOP;
    private short maxGOP;
    private short bPictureCount;
    private int bitRate;
    private byte profileAndLevel;

    public MPEG2VideoDescriptor(UL ul) {
        super(ul);
    }

    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {

            case 0x8000:
                singleSequence = _bb.get();
                break;
            case 0x8001:
                constantBFrames = _bb.get();
                break;
            case 0x8002:
                codedContentType = _bb.get();
                break;
            case 0x8003:
                lowDelay = _bb.get();
                break;
            case 0x8004:
                closedGOP = _bb.get();
                break;
            case 0x8005:
                identicalGOP = _bb.get();
                break;
            case 0x8006:
                maxGOP = _bb.getShort();
                break;
            case 0x8007:
                bPictureCount = (short) (_bb.get() & 0xff);
                break;
            case 0x8008:
                bitRate = _bb.getInt();
                break;
            case 0x8009:
                profileAndLevel = _bb.get();
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x + (" + _bb.remaining() + ")", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public byte getSingleSequence() {
        return singleSequence;
    }

    public byte getConstantBFrames() {
        return constantBFrames;
    }

    public byte getCodedContentType() {
        return codedContentType;
    }

    public byte getLowDelay() {
        return lowDelay;
    }

    public byte getClosedGOP() {
        return closedGOP;
    }

    public byte getIdenticalGOP() {
        return identicalGOP;
    }

    public short getMaxGOP() {
        return maxGOP;
    }

    public short getbPictureCount() {
        return bPictureCount;
    }

    public int getBitRate() {
        return bitRate;
    }

    public byte getProfileAndLevel() {
        return profileAndLevel;
    }
}