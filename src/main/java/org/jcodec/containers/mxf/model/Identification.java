package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

import js.nio.ByteBuffer;
import js.util.Date;
import js.util.Map;
import js.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Identification extends MXFInterchangeObject {

    private UL thisGenerationUID;
    private String companyName;
    private String productName;
    private short versionString;
    private UL productUID;
    private Date modificationDate;
    private String platform;

    public Identification(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {

            case 0x3c09:
                thisGenerationUID = UL.read(_bb);
                break;

            case 0x3c01:
                companyName = readUtf16String(_bb);
                break;

            case 0x3c02:
                productName = readUtf16String(_bb);
                break;

            case 0x3c04:
                versionString = _bb.getShort();
                break;

            case 0x3c05:
                productUID = UL.read(_bb);
                break;

            case 0x3c06:
                modificationDate = readDate(_bb);
                break;

            case 0x3c08:
                platform = readUtf16String(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL getThisGenerationUID() {
        return thisGenerationUID;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getProductName() {
        return productName;
    }

    public short getVersionString() {
        return versionString;
    }

    public UL getProductUID() {
        return productUID;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public String getPlatform() {
        return platform;
    }
}
