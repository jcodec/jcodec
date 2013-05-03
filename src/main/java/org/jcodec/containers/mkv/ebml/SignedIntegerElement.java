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
 * SignedInteger.java
 *
 * Created on February 17, 2003, 1:54 PM
 */

/**
 * Basic class for the Signed Integer EBML data type.
 * 
 * @author John Cannon
 */
public class SignedIntegerElement extends BinaryElement {

    public SignedIntegerElement(byte[] typeID) {
        super(typeID);
    }

    public void setValue(long value) {
        setData(convertToBytes(value));
    }

    public long getValue() {
        if ((data.limit()-data.position()) == 8)
            return data.duplicate().getLong();
        
        byte[] b = data.array();
        long l = 0;
        for (int i = b.length-1; i >=0 ; i--)
          l |= (b[i] & 0xFFL) << (8*(b.length-1-i));

        return l;
    }

    /**
     * TODO: This should be replaces with a straight-forward calculation
     * 
     * @param val
     * @return
     */
    public static int getSerializedSize(long val) {
        if (val <= 0x40 && val >= (-0x3F)) {
            return 1;
        } else if (val <= 0x2000 && val >= (-0x1FFF)) {
            return 2;
        } else if (val <= 0x100000 && val >= (-0x0FFFFF)) {
            return 3;
        } else if (val <= 0x8000000 && val >= (-0x07FFFFFF)) {
            return 4;
        } else if (val <= 0x400000000L && val >= -0x03FFFFFFFFL) {
            return 5;
        } else if (val <= 0x20000000000L && val >= -0x01FFFFFFFFFFL) {
            return 6;
        } else if (val <= 0x1000000000000l && val >= -0x00FFFFFFFFFFFFL) {
            return 7;
        } else {
            return 8;
        }
    }

    public static final long[] signedComplement = { 0, 0x3F, 0x1FFF, 0x0FFFFF, 0x07FFFFFF, 0x03FFFFFFFFL, 0x01FFFFFFFFFFL, 0x00FFFFFFFFFFFFL, 0x007FFFFFFFFFFFFFL };

    public static long convertToUnsigned(long val) {
        return val + signedComplement[getSerializedSize(val)];
    }

    public static byte[] convertToBytes(long val) {
        int num = getSerializedSize(val);
        val += signedComplement[num];
//        Assert.assertEquals("Int size changed after adding compliment, shouldn't happen.", num, getSerializedSize(val));

        return Element.ebmlBytes(val, num);
    }

    
}
