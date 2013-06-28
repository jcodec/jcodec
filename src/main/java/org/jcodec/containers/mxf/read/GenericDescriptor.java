package org.jcodec.containers.mxf.read;

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
public class GenericDescriptor extends MXFMetadataSet {
    private UL[] subDescr;
    private UL containerUl;
    private int linkedTrackId;
    private UL pictureUl;
    private int width;
    private int height;
    private int arNum;
    private int arDen;
    private int srNum;
    private int srDen;
    private UL soundUl;
    private int channels;
    private int bitsPerSample;
    
    public GenericDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3F01:
                subDescr = readULs(_bb);
                break;
            case 0x3004:
                containerUl = UL.read(_bb);
                break;
            case 0x3006:
                linkedTrackId = _bb.getInt();
                break;
            case 0x3201: /* PictureEssenceCoding */
                pictureUl = UL.read(_bb);
                break;
            case 0x3203:
                width = _bb.getInt();
                break;
            case 0x3202:
                height = _bb.getInt();
                break;
            case 0x320E:
                arNum = _bb.getInt();
                arDen = _bb.getInt();
                break;
            case 0x3D03:
                srNum = _bb.getInt();
                srDen = _bb.getInt();
                break;
            case 0x3D06: /* SoundEssenceCompression */
                soundUl = UL.read(_bb);
                break;
            case 0x3D07:
                channels = _bb.getInt();
                break;
            case 0x3D01:
                bitsPerSample = _bb.getInt();
                break;
            case 0x3401:
                // mxf_read_pixel_layout(_bb);
                break;
            default:
                System.out.println(String.format("Unknown label: %04x", entry.getKey()));
            }
        }
    }

    public UL[] getSubDescr() {
        return subDescr;
    }

    public UL getContainerUl() {
        return containerUl;
    }

    public int getLinkedTrackId() {
        return linkedTrackId;
    }

    public UL getPictureUl() {
        return pictureUl;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getArNum() {
        return arNum;
    }

    public int getArDen() {
        return arDen;
    }

    public int getSrNum() {
        return srNum;
    }

    public int getSrDen() {
        return srDen;
    }

    public UL getSoundUl() {
        return soundUl;
    }

    public int getChannels() {
        return channels;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }
}