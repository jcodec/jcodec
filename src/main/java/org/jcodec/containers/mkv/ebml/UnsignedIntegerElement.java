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
 * UnsignedIntegerElement.java
 *
 * Created on February 15, 2003, 6:27 PM
 */
/**
 * Basic class for the Unsigned Integer data type in EBML.
 * 
 * @author John Cannon
 */
public class UnsignedIntegerElement extends BinaryElement {

    public UnsignedIntegerElement(byte[] typeID) {
        super(typeID);
    }

    public UnsignedIntegerElement(byte[] typeID, long value) {
        super(typeID);
        set(value);
    }

    public void set(long value) {
        setData(longToBytes(value));
    }

    public static byte[] longToBytes(long value) {
        byte[] b = new byte[getMinByteSizeUnsigned(value)];
        for (int i = b.length - 1; i >= 0; i--) {
            b[i] = (byte) (value >>> (8 * (b.length - i - 1)));
        }
        return b;
    }

    public static int getMinByteSizeUnsigned(long value) {
        int size = 8;
        long mask = 0xFF00000000000000L;
        for (int i = 0; i < 8; i++) {
            if ((value & mask) == 0) {
                mask = mask >>> 8;
                size--;
            } else {
                return size;
            }
        }
        return 1;
    }

    public long get() {
        
        long l = 0;
        long tmp = 0;
        for (int i = 0; i < data.limit(); i++) {
            tmp = ((long) data.get(data.limit() - 1 - i)) << 56;
            tmp >>>= (56 - (i * 8));
            l |= tmp;
        }

        return l;
    }
    

}
