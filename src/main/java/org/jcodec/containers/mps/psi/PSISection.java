package org.jcodec.containers.mps.psi;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents a section of PSI payload ( Program Stream Information ) MPEG
 * Transport stream
 * 
 * @author The JCodec project
 * 
 */
public class PSISection {
    private int tableId;
    private int specificId;
    private int versionNumber;
    private int currentNextIndicator;
    private int sectionNumber;
    private int lastSectionNumber;

    /**
     * A copy constructor
     * 
     * @param other
     */
    public PSISection(PSISection other) {
        this(other.tableId, other.specificId, other.versionNumber, other.currentNextIndicator, other.sectionNumber,
                other.lastSectionNumber);
    }

    public PSISection(int tableId, int specificId, int versionNumber, int currentNextIndicator, int sectionNumber,
            int lastSectionNumber) {
        this.tableId = tableId;
        this.specificId = specificId;
        this.versionNumber = versionNumber;
        this.currentNextIndicator = currentNextIndicator;
        this.sectionNumber = sectionNumber;
        this.lastSectionNumber = lastSectionNumber;
    }

    public static PSISection parsePSI(ByteBuffer data) {
        int tableId = data.get() & 0xff;
        int w0 = data.getShort() & 0xffff;
        if ((w0 & 0xC000) != 0x8000)
            throw new RuntimeException("Invalid section data");

        int sectionLength = w0 & 0xfff;

        data.limit(data.position() + sectionLength);

        int specificId = data.getShort() & 0xffff;
        int b0 = data.get() & 0xff;
        int versionNumber = (b0 >> 1) & 0x1f;
        int currentNextIndicator = b0 & 1;

        int sectionNumber = data.get() & 0xff;
        int lastSectionNumber = data.get() & 0xff;

        return new PSISection(tableId, specificId, versionNumber, currentNextIndicator, sectionNumber,
                lastSectionNumber);
    }

    public int getTableId() {
        return tableId;
    }

    public int getSpecificId() {
        return specificId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public int getCurrentNextIndicator() {
        return currentNextIndicator;
    }

    public int getSectionNumber() {
        return sectionNumber;
    }

    public int getLastSectionNumber() {
        return lastSectionNumber;
    }
}