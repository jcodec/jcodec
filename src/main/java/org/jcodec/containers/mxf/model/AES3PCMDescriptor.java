package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

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
public class AES3PCMDescriptor extends WaveAudioDescriptor {
    private byte emphasis;
    private short blockStartOffset;
    private byte auxBitsMode;
    private ByteBuffer channelStatusMode;
    private ByteBuffer fixedChannelStatusData;
    private ByteBuffer userDataMode;
    private ByteBuffer fixedUserData;

    public AES3PCMDescriptor(UL ul) {
        super(ul);
    }

    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3d0d:
                emphasis = _bb.get();
                break;
            case 0x3d0f:
                blockStartOffset = _bb.getShort();
                break;
            case 0x3d08:
                auxBitsMode = _bb.get();
                break;
            case 0x3d10:
                channelStatusMode = _bb;
                break;
            case 0x3d11:
                fixedChannelStatusData = _bb;
                break;
            case 0x3d12:
                userDataMode = _bb;
                break;
            case 0x3d13:
                fixedUserData = _bb;
                break;

            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public byte getEmphasis() {
        return emphasis;
    }

    public short getBlockStartOffset() {
        return blockStartOffset;
    }

    public byte getAuxBitsMode() {
        return auxBitsMode;
    }

    public ByteBuffer getChannelStatusMode() {
        return channelStatusMode;
    }

    public ByteBuffer getFixedChannelStatusData() {
        return fixedChannelStatusData;
    }

    public ByteBuffer getUserDataMode() {
        return userDataMode;
    }

    public ByteBuffer getFixedUserData() {
        return fixedUserData;
    }
}