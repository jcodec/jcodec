package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class RGBAEssenceDescriptor extends GenericPictureEssenceDescriptor {
    private int componentMaxRef;
    private int componentMinRef;
    private int alphaMaxRef;
    private int alphaMinRef;
    private byte scanningDirection;
    private ByteBuffer pixelLayout;
    private ByteBuffer palette;
    private ByteBuffer paletteLayout;

    public RGBAEssenceDescriptor(UL ul) {
        super(ul);
    }

    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3406:
                componentMaxRef = _bb.getInt();
                break;
            case 0x3407:
                componentMinRef = _bb.getInt();
                break;
            case 0x3408:
                alphaMaxRef = _bb.getInt();
                break;
            case 0x3409:
                alphaMinRef = _bb.getInt();
                break;
            case 0x3405:
                scanningDirection = _bb.get();
                break;
            case 0x3401:
                pixelLayout = _bb;
                break;
            case 0x3403:
                palette = _bb;
                break;
            case 0x3404:
                paletteLayout = _bb;
                break;
            default:
                System.out.println(String.format("Unknown tag [ RGBAEssenceDescriptor: " + ul + "]: %04x",
                        entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public int getComponentMaxRef() {
        return componentMaxRef;
    }

    public int getComponentMinRef() {
        return componentMinRef;
    }

    public int getAlphaMaxRef() {
        return alphaMaxRef;
    }

    public int getAlphaMinRef() {
        return alphaMinRef;
    }

    public byte getScanningDirection() {
        return scanningDirection;
    }

    public ByteBuffer getPixelLayout() {
        return pixelLayout;
    }

    public ByteBuffer getPalette() {
        return palette;
    }

    public ByteBuffer getPaletteLayout() {
        return paletteLayout;
    }
}