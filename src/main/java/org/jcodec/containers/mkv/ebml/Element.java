/**
 * JEBML - Java library to read/write EBML/Matroska elements.
 * Copyright (C) 2004 Jory Stone <jebml@jory.info>
 * Based on Javatroska (C) 2002 John Cannon <spyder@matroska.org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jcodec.containers.mkv.ebml;

/*
 * Element.java
 *
 * Created on November 19, 2002, 9:11 PM
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import junit.framework.Assert;

import org.jcodec.containers.mkv.Type;

/**
 * Defines the basic EBML element. Subclasses may provide child element access.
 * 
 * @author John Cannon
 */
public class Element {

    protected Element parent;
    public Type type;
    public byte[] id = { 0x00 };
    public long size = 0;
    protected ByteBuffer data;
    protected boolean dataRead = false;
    public long offset;
    public long dataOffset;

    /** Creates a new instance of Element */
    public Element(byte[] type) {
        this.id = type;
    }

    /**
     * Read the element data
     * 
     * @throws IOException
     */
    public void readData(FileChannel source) throws IOException {
        // Setup a buffer for it's data
        try {
            this.data = ByteBuffer.allocate((int) size);

            // Read the data
            while (source.read(data) != -1 && data.hasRemaining())
                ;
            dataRead = true;
            this.data.flip();
        } catch (OutOfMemoryError oome) {
            System.out.println("OutOfMemoryError while trying to read " + size + " bytes for element at " + offset);
            throw oome;
        }
    }

    /**
     * Skip the element data
     * @throws IOException 
     */
    public void skipData(FileChannel source) throws IOException {
        if (!dataRead) {
            // Skip the data
            source.position(dataOffset+size);
            dataRead = true;
        }
    }

    /**
     * Get the total size of this element
     */
    public long getSize() {
        long totalSize = 0;
        if (data != null && data.limit() > 0) {
            // If possible, get real data size
            // First count the real data size
            totalSize += data.limit();
            // Then add the encoded lengh of data
            totalSize += Element.getEbmlSize(data.limit());
            
        } else {
            //if no data is present, try the same with size attribute
            totalSize += this.size;
            totalSize += Element.getEbmlSize(size);
        }
        totalSize += id.length;
        return totalSize;
    }

    /**
     * Getter for property type.
     * 
     * @return Value of property type.
     * 
     */
    public byte[] getId() {
        return id;
    }

    /**
     * Setter for property parent.
     * 
     * @param parent
     *            New value of property parent.
     * 
     */
    public void setParent(Element parent) {
        this.parent = parent;
    }

    public boolean isSameEbmlId(byte[] typeId) {
        return Arrays.equals(this.id, typeId);
    }

    public boolean isSameMatroskaType(Type elemType) {
        return this.isSameEbmlId(elemType.id);
    }

    public long mux(FileChannel os) throws IOException {
        ByteBuffer bb = mux();
        return os.write(bb);
    }
    
    public ByteBuffer mux() {
        int sizeSize = getEbmlSize(data.limit());
        ByteBuffer bb = ByteBuffer.allocate(id.length + sizeSize + data.limit());
        bb.put(id);

        byte[] size = ebmlBytes(data.limit(), sizeSize);
        bb.put(size);

        Assert.assertEquals(this.type+" data seems to be read already or not ready for reading", 0, data.position());
        Assert.assertEquals(data.capacity(), data.limit());
        bb.put(data);
        bb.flip();
        data.flip();
        
        return bb;
    }

    public static byte[] ebmlBytes(long value) {
        int size = getEbmlSize(value);
        return ebmlBytes(value, size);
    }

    public static byte[] ebmlBytes(long val, int num) {
        byte[] b = new byte[num];
        for (int idx = 0; idx < num; idx++) {
            // Rightmost bytes should go to end of array to preserve big-endian
            // notation
            b[num - idx - 1] = (byte) ((val >>> (8 * idx)) & 0xFFL);
        }

        b[0] |= 0x80 >>> (num - 1);
        return b;
    }

    public static int getEbmlSize(long v) {
        if (v == 0)
            return 1;
        
        long oneMask = 0x7F;
        // 0x3F 0x80
        long twoMask = 0x3F80;
        // 0x1F 0xC0 0x00
        long threeMask = 0x1FC000;
        // 0x0F 0xE0 0x00 0x00
        long fourMask = 0x0FE00000;
        // 0x07 0xF0 0x00 0x00 0x00
        long fiveMask = 0x07F0000000L;
        // 0x03 0xF8 0x00 0x00 0x00 0x00
        long sixMask = 0x03F800000000L; 
        // 0x01 0xFC 0x00 0x00 0x00 0x00 0x00
        long sevenMask = 0x01FC0000000000L;
        // 0x00 0xFE 0x00 0x00 0x00 0x00 0x00  0x00
        long eightMask = 0xFE000000000000L;
        long[] allMasks = new long[] { 0, oneMask, twoMask, threeMask, fourMask, fiveMask, sixMask, sevenMask, eightMask };
        int size = 8;

        while (size > 0 && (v & allMasks[size]) == 0)
            size--;

        return size;
    }
}
