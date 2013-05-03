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
package org.jcodec.containers.mkv;

import static java.lang.Integer.toHexString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jcodec.containers.mkv.ebml.Element;

/**
 * EBMLReader.java
 *
 * Created on November 18, 2002, 4:03 PM
 *
 * @version 1.0
 */

/**
 * <h1>JEBML Intro</h1>
 * <hr>
 * <p>
 * The following are the basic steps of how reading in JEBML works.
 * </p>
 * <ul>
 * <li>1. The EbmlReader class reads the element header (id+size) and looks it
 * up in the supplied DocType class.
 * <li>2. The correct element type (Binary, UInteger, String, etc) is created
 * using the DocType data, BinaryElement is default element type for unknown
 * elements.
 * <li>3. The MatroskaDocType has the ids of all the elements staticly declared.
 * <br>
 * So to easily find out what an element is, you can use some code like the
 * following code
 * <p>
 * <code>
 * Element level1; <br>
 * // ... fill level1 <br>
 * if (level1.equals(MatroskaDocType.SegmentInfo_Id)) { <br>
 *   // Your Code Here <br>
 * } <br>
 * </code>
 * </p>
 * <li>4. To get the actual data for an Element you call the readData method, if
 * you just want to skip it use skipData().
 * <li>5. MasterElements are special, they have the readNextChild() method which
 * returns the next child element, returning null when all the children have
 * been read (it keeps track of the current inputstream position).
 * <li>The usage method for JEBML is very close to libebml/libmatroska.
 * </ul>
 * <hr>
 * 
 * Reads EBML elements from a <code>DataSource</code> and looks them up in the
 * provided <code>DocType</code>.
 * 
 * @author (c) 2002 John Cannon
 * @author (c) 2004 Jory Stone
 */
public class Reader {

    protected FileChannel ds;

    /**
     * Creates a new <code>EBMLReader</code> reading from the <code>DataSource
     * source</code>. The <code>DocType doc</code> is used to validate the
     * document.
     * 
     * @param source
     *            DataSource to read from
     * @param doc
     *            DocType to use to validate the docment
     */
    public Reader(FileChannel source) {
        this.ds = source;
    }

    public long getPos() throws IOException {
        return 0;
    }
    
    public long getAvailable() throws IOException {
        return 0;
    }

    public static String bytesToHex(byte[] bts) {
        StringBuilder sb = new StringBuilder();
        if (bts == null)
            return "";

        for (byte b : bts) {
            sb.append(" 0x").append(toHexString(b & 0xFF).toUpperCase());
        }
        return sb.toString();
    }
    
    public static String printAsHex(byte[] a){
        StringBuilder sb = new StringBuilder();
        for(byte b: a)
           sb.append(String.format("0x%02x ", b&0xff));
        return sb.toString();
    }

    public Element readNextElement() throws IOException {
        // Read the type.
        long offset = ds.position();
        byte[] typeId = getRawEbmlBytes(ds);

        if (typeId == null)
            // Failed to read type id
            // one byte was read from source (this should be reflected in
            // 'usedSize' of a master element)
            return null;

        // Read the size.
        byte[] ebmlCodedElementSize = getEbmlBytes(ds);
        long size = bytesToLong(ebmlCodedElementSize);

        // Zero sized element is valid
        if (size == 0)
            ;

        // according to MatroskaDocType.createElement return value is never
        // 'null'
        Element elem = Type.createElementById(typeId);
        elem.offset = offset;

        // Set it's size
        elem.size = size;

//        if (ebmlCodedElementSize == null) {
//            elem.setHeaderSize(1); // if data array is null, only one byte was read;
//        } else {
//            elem.setHeaderSize(ebmlCodedElementSize.length);
//        }

        return elem;
    }

    /**
     * Reads an (Unsigned) EBML code from the DataSource and encodes it into a
     * long. This size should be cast into an int for actual use as Java only
     * allows upto 32-bit file I/O operations.
     * 
     * @return ebml size
     * @throws IOException 
     */
    static public long getEbmlVInt(FileChannel source) throws IOException {
        // Begin loop with byte set to newly read byte.
        ByteBuffer bufferForFirstByte = ByteBuffer.allocate(1);
        source.read(bufferForFirstByte);
        bufferForFirstByte.flip();
        byte firstByte = bufferForFirstByte.get();
        int numBytes = getEbmlSizeByFirstByte(firstByte);

        if (numBytes == 0)
            // Invalid size
            return 0;
        
        if (numBytes == 1)
            return firstByte & ((0xFF >>> (numBytes)));

        // Setup space to store the bits
        ByteBuffer bb = ByteBuffer.allocate(numBytes);
        

        // Clear the 1 at the front of this byte, all the way to the beginning
        // of the size
        bb.put((byte) (firstByte & ((0xFF >>> (numBytes)))));

        // Read the rest of the size.
        source.read(bb);

        // Put this into a long
        return bytesToLong(bb.array());
    }
    
    /**
     * Reads an Signed EBML code from the DataSource and encodes it into a long.
     * This size should be cast into an int for actual use as Java only allows
     * upto 32-bit file I/O operations.
     * 
     * @return ebml size
     * @throws IOException 
     */
    static public long getSignedEbmlVInt(FileChannel is) throws IOException {
        // Begin loop with byte set to newly read byte.
        byte[] vInt = getEbmlBytes(is);
        long uInt = bytesToLong(vInt);
        
        return uInt - signedComplement[vInt.length];
    }
    
    public static long bytesToLong(ByteBuffer data) {
        if (data == null)
            return 0;

        long value = 0;
        while(data.position() < data.limit())
            value = (value << 8) | (data.get() & 0xff);

        return value;
    }

    public static long bytesToLong(byte[] data) {
        if (data == null)
            return 0;

        long value = 0;
        for (int i = 0; i < data.length; i++)
            value = (value << 8) | (data[i] & 0xff);

        return value;
    }
    
    
    static public int getEbmlSizeByFirstByte(byte b){
        int numBytes = 0;

        // Begin by counting the bits unset before the first '1'.
        long mask = 0x0080;
        for (int i = 0; i < 8; i++) {
            // Start at left, shift to right.
            if ((b & mask) == mask) { // One found
                                              // Set number of bytes in size =
                                              // i+1 ( we must count the 1 too)
                numBytes = i + 1;
                // exit loop by pushing i out of the limit
                i = 8;
            }
            mask >>>= 1;
        }
        return numBytes;
    }
    
    /**
     * Reads an (Unsigned) EBML code from the DataSource and encodes it into a
     * long. This size should be cast into an int for actual use as Java only
     * allows upto 32-bit file I/O operations.
     * 
     * @return ebml size
     */
    static public long getEbmlVInt(ByteBuffer bb) {
        // Begin loop with byte set to newly read byte.
        byte firstByte = bb.get();
        int numBytes = getEbmlSizeByFirstByte(firstByte);
        if (numBytes == 0)
            // Invalid size
            return 0;
        
        
        if (numBytes == 1)
            return firstByte & ((0xFF >>> (numBytes)));
        
        // Setup space to store the bits
        
        byte[] data = new byte[numBytes];

        // Clear the 1 at the front of this byte, all the way to the beginning
        // of the size
        data[0] = (byte) (firstByte & ((0xFF >>> (numBytes))));

        // Read the rest of the size.
        bb.get(data, 1, data.length - 1);
        
        long longValue = bytesToLong(data);

        // Put this into a long
        return longValue;
    }
    
    /**
     * Reads an (Unsigned) EBML code from the DataSource and encodes it into a
     * long. This size should be cast into an int for actual use as Java only
     * allows upto 32-bit file I/O operations.
     * 
     * @return ebml size
     */
    static public long getEbmlVInt(ByteBuffer source, int offset) {
        // Begin loop with byte set to newly read byte.
        byte firstByte = source.get(offset);
        int numBytes = getEbmlSizeByFirstByte(firstByte);
        if (numBytes == 0)
            // Invalid size
            return 0;
        
        
        if (numBytes == 1)
            return firstByte & ((0xFF >>> (numBytes)));
        
        // Setup space to store the bits
        byte[] data = new byte[numBytes];
        

        // Clear the 1 at the front of this byte, all the way to the beginning
        // of the size
        data[0] = (byte) (firstByte & ((0xFF >>> (numBytes))));

        // Read the rest of the size.
        System.arraycopy(source.array(), offset + 1, data, 1, numBytes - 1);
        
        long longValue = bytesToLong(data);

        // Put this into a long
        return longValue;
    }
    
    public static final long[] signedComplement = {0, 0x3F, 0x1FFF, 0x0FFFFF, 0x07FFFFFF, 0x03FFFFFFFFL, 0x01FFFFFFFFFFL, 0x00FFFFFFFFFFFFL, 0x007FFFFFFFFFFFFFL};

    public static String asHex(int i){
        return "0x"+printAsHex(new byte[]{(byte)i});
    }
    
    /**
     * Reads an Signed EBML code from the DataSource and encodes it into a long.
     * This size should be cast into an int for actual use as Java only allows
     * upto 32-bit file I/O operations.
     * 
     * @return ebml size
     */
    /**
     * Reads an Signed EBML code from the DataSource and encodes it into a long.
     * This size should be cast into an int for actual use as Java only allows
     * upto 32-bit file I/O operations.
     * 
     * @return ebml size
     */
    static public long getSignedEbmlVInt(ByteBuffer source) {
        // Begin loop with byte set to newly read byte.
        byte[] bytes = getVIntEbmlBytes(source);
        
        if (bytes == null)
            throw new RuntimeException("Can't convert byte 0x"+Integer.toHexString(source.get(source.position()-1) & 0xFF).toUpperCase()+" to first ebml byte.");
        
        return bytesToLong(bytes) - signedComplement[bytes.length];
    }

//    /**
//     * Reads an Signed EBML code from the DataSource and encodes it into a long.
//     * This size should be cast into an int for actual use as Java only allows
//     * upto 32-bit file I/O operations.
//     * 
//     * @return ebml size
//     * @throws IOException 
//     */
//    static public long readSignedEBMLCode(FileChannel source) throws IOException {
//
//        // Begin loop with byte set to newly read byte.
//        ByteBuffer bufferForFirstByte = ByteBuffer.allocate(1);
//        source.read(bufferForFirstByte);
//        bufferForFirstByte.flip();
//        byte firstByte = bufferForFirstByte.get();
//        int numBytes = 0;
//
//        // Begin by counting the bits unset before the first '1'.
//        long mask = 0x0080;
//        for (int i = 0; i < 8; i++) {
//            // Start at left, shift to right.
//            if ((firstByte & mask) == mask) { // One found
//                // Set number of bytes in size = i+1 ( we must count the 1 too)
//                numBytes = i + 1;
//                // exit loop by pushing i out of the limit
//                i = 8;
//            }
//            mask >>>= 1;
//        }
//        if (numBytes == 0)
//            // Invalid size
//            return 0;
//
//        // Setup space to store the bits
//        byte[] data = new byte[numBytes];
//
//        // Clear the 1 at the front of this byte, all the way to the beginning
//        // of the size
//        data[0] = (byte) (firstByte & ((0xFF >>> (numBytes))));
//
//        if (numBytes > 1) {
//            // Read the rest of the size.
//            source.read(data, 1, numBytes - 1);
//        }
//
//        // Put this into a long
//        long size = 0;
//        long n = 0;
//        for (int i = 0; i < numBytes; i++) {
//            n = ((long) data[numBytes - 1 - i] << 56) >>> 56;
//            size = size | (n << (8 * i));
//        }
//
//        // Sign it ;)
//        if (numBytes == 1) {
//            size -= 63;
//
//        } else if (numBytes == 2) {
//            size -= 8191;
//
//        } else if (numBytes == 3) {
//            size -= 1048575;
//
//        } else if (numBytes == 4) {
//            size -= 134217727;
//        }
//
//        return size;
//    }

    static public byte[] getVIntEbmlBytes(ByteBuffer source) {
        // Begin loop with byte set to newly read byte.

        byte firstByte = source.get();
        int numBytes = getEbmlSizeByFirstByte(firstByte);
        
        if (numBytes == 0)
            // Invalid size
            return null;
        
        // Setup space to store the bits
        ByteBuffer data = ByteBuffer.allocate(numBytes);
        
        // Clear the 1 at the front of this byte, all the way to the beginning of the size
        data.put((byte) (firstByte & ((0xFF >>> (numBytes)))));
        
        // Read the rest of the size.
        int remaining = numBytes-1;
        while(remaining > 0){
            data.put(source.get());
            remaining--;
        }

        return data.array();
    }
    
    static public byte[] getEbmlBytes(FileChannel source) throws IOException {
        // Begin loop with byte set to newly read byte.
        ByteBuffer bufferForFirstByte = ByteBuffer.allocate(1);
        source.read(bufferForFirstByte);
        bufferForFirstByte.flip();
        byte firstByte = (byte) bufferForFirstByte.get();
        
        int numBytes = getEbmlSizeByFirstByte(firstByte);
        
        if (numBytes == 0)
            // Invalid size
            return null;
        
        if (numBytes == 1)
            return  new byte[]{(byte) (firstByte & ((0xFF >>> (numBytes))))};

        // Setup space to store the bits
        ByteBuffer data = ByteBuffer.allocate(numBytes);
        
        // Clear the 1 at the front of this byte, all the way to the beginning
        // of the size
        data.put((byte) (firstByte & ((0xFF >>> (numBytes)))));


        // Read the rest of the size.
        source.read(data);

        return data.array();
    }
    /**
     * Reads an EBML id from the DataSource. EBML ids have length encoded inside
     * of them For instance, all one-byte ids have first byte set to '1', like
     * 0xA3, 0xE7, whereas the two-byte ids have first byte set to '0' and
     * second byte set to '1', thus: 0x4286 (ebml version) or 0x42F7 (ebml read
     * version)
     * 
     * @return byte array filled with the ebml size, (size bits included)
     * @throws IOException 
     */
    static public byte[] getRawEbmlBytes(FileChannel source) throws IOException {
        // Begin loop with byte set to newly read byte.
        if (source.position() == source.size())
            return null;
        
        ByteBuffer bufferForFirstByte = ByteBuffer.allocate(1);
        source.read(bufferForFirstByte);
        bufferForFirstByte.flip();
        
        byte firstByte = bufferForFirstByte.get();
        int numBytes = getEbmlSizeByFirstByte(firstByte);
        
        if (numBytes == 0)
            // Invalid element
            return null;
        
        if (numBytes == 1)
            return bufferForFirstByte.array();
        
        // Setup space to store the bits
        ByteBuffer data = ByteBuffer.allocate(numBytes);
        data.put(firstByte);

        // Clear the 1 at the front of this byte, all the way to the beginning
        // of the size
        

        // Read the rest of the size.
        
        source.read(data);
        return data.array();
    }

    public Element readNextFirstLevelElement() throws IOException {
        if (ds.position() == ds.size())
            return null;
        
        long offset = ds.position();
        byte[] typeId = getRawEbmlBytes(ds);
        while (typeId == null || !Type.isFirstLevelHeader(typeId)){
            offset++;
            ds.position(offset);
            typeId = getRawEbmlBytes(ds);
        }
        
        byte[] data = getEbmlBytes(ds);
        long elementSize = bytesToLong(data);
        Element elem = Type.createElementById(typeId);
        elem.size = elementSize;
        elem.offset = offset;
        return elem;
    }

}
