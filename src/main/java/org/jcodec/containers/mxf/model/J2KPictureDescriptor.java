package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.logging.Logger;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class J2KPictureDescriptor extends MXFInterchangeObject {

    private short rsiz;
    private int xsiz;
    private int ysiz;
    private int xOsiz;
    private int yOsiz;
    private int xTsiz;
    private int yTsiz;
    private int xTOsiz;
    private int yTOsiz;
    private short csiz;

    public J2KPictureDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x6104:
                rsiz = _bb.getShort();
                break;
            case 0x6105:
                xsiz = _bb.getInt();
                break;
            case 0x6106:
                ysiz = _bb.getInt();
                break;
            case 0x6107:
                xOsiz = _bb.getInt();
                break;
            case 0x6108:
                yOsiz = _bb.getInt();
                break;
            case 0x6109:
                xTsiz = _bb.getInt();
                break;
            case 0x610a:
                yTsiz = _bb.getInt();
                break;
            case 0x610b:
                xTOsiz = _bb.getInt();
                break;
            case 0x610c:
                yTOsiz = _bb.getInt();
                break;
            case 0x610d:
                csiz = _bb.getShort();
                break;

            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public short getRsiz() {
        return rsiz;
    }

    public int getXsiz() {
        return xsiz;
    }

    public int getYsiz() {
        return ysiz;
    }

    public int getxOsiz() {
        return xOsiz;
    }

    public int getyOsiz() {
        return yOsiz;
    }

    public int getxTsiz() {
        return xTsiz;
    }

    public int getyTsiz() {
        return yTsiz;
    }

    public int getxTOsiz() {
        return xTOsiz;
    }

    public int getyTOsiz() {
        return yTOsiz;
    }

    public short getCsiz() {
        return csiz;
    }
}
