package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Rational;

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
public class GenericSoundEssenceDescriptor extends FileDescriptor {
    private Rational audioSamplingRate;
    private byte locked;
    private byte audioRefLevel;
    private byte electroSpatialFormulation;
    private int channelCount;
    private int quantizationBits;
    private byte dialNorm;
    private UL soundEssenceCompression;

    public GenericSoundEssenceDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3d03:
                audioSamplingRate = new Rational(_bb.getInt(), _bb.getInt());
                break;
            case 0x3d02:
                locked = _bb.get();
                break;
            case 0x3d04:
                audioRefLevel = _bb.get();
                break;
            case 0x3d05:
                electroSpatialFormulation = _bb.get();
                break;
            case 0x3d07:
                channelCount = _bb.getInt();
                break;
            case 0x3d01:
                quantizationBits = _bb.getInt();
                break;
            case 0x3d0c:
                dialNorm = _bb.get();
                break;
            case 0x3d06:
                soundEssenceCompression = UL.read(_bb);
                break;

            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public Rational getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public byte getLocked() {
        return locked;
    }

    public byte getAudioRefLevel() {
        return audioRefLevel;
    }

    public byte getElectroSpatialFormulation() {
        return electroSpatialFormulation;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getQuantizationBits() {
        return quantizationBits;
    }

    public byte getDialNorm() {
        return dialNorm;
    }

    public UL getSoundEssenceCompression() {
        return soundEssenceCompression;
    }
}