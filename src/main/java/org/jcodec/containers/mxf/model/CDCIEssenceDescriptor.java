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
public class CDCIEssenceDescriptor extends GenericPictureEssenceDescriptor {

    private int componentDepth;
    private int horizontalSubsampling;
    private int verticalSubsampling;
    private byte colorSiting;
    private byte reversedByteOrder;
    private short paddingBits;
    private int alphaSampleDepth;
    private int blackRefLevel;
    private int whiteReflevel;
    private int colorRange;
    
    public CDCIEssenceDescriptor(UL ul) {
        super(ul);
    }

    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3301:
                componentDepth = _bb.getInt();
                break;
            case 0x3302:
                horizontalSubsampling = _bb.getInt();
                break;
            case 0x3308:
                verticalSubsampling = _bb.getInt();
                break;
            case 0x3303:
                colorSiting = _bb.get();
                break;
            case 0x330b:
                reversedByteOrder = _bb.get();
                break;
            case 0x3307:
                paddingBits = _bb.getShort();
                break;
            case 0x3309:
                alphaSampleDepth = _bb.getInt();
                break;
            case 0x3304:
                blackRefLevel = _bb.getInt();
                break;
            case 0x3305:
                whiteReflevel = _bb.getInt();
                break;
            case 0x3306:
                colorRange = _bb.getInt();
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public int getComponentDepth() {
        return componentDepth;
    }

    public int getHorizontalSubsampling() {
        return horizontalSubsampling;
    }

    public int getVerticalSubsampling() {
        return verticalSubsampling;
    }

    public byte getColorSiting() {
        return colorSiting;
    }

    public byte getReversedByteOrder() {
        return reversedByteOrder;
    }

    public short getPaddingBits() {
        return paddingBits;
    }

    public int getAlphaSampleDepth() {
        return alphaSampleDepth;
    }

    public int getBlackRefLevel() {
        return blackRefLevel;
    }

    public int getWhiteReflevel() {
        return whiteReflevel;
    }

    public int getColorRange() {
        return colorRange;
    }
}