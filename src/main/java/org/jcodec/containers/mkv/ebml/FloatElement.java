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

import java.nio.ByteBuffer;

public class FloatElement extends BinaryElement {
    public FloatElement(byte[] type) {
        super(type);
    }

    /**
     * Set the float value of this element
     * 
     * @param value
     *            Float value to set
     * @throws ArithmeticException
     *             if the float value is larger than Double.MAX_VALUE
     */
    public void set(double value) {
        if (value < Float.MAX_VALUE) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putFloat((float) value);
            bb.flip();
            this.data = bb;

        } else if (value < Double.MAX_VALUE) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putDouble(value);
            bb.flip();
            this.data = bb;

        } else {
            throw new ArithmeticException("80-bit floats are not supported, BTW How did you create such a large float in Java?");
        }
    }

    /**
     * Get the float value of this element
     * 
     * @return Float value of this element
     * @throws ArithmeticException
     *             for 80-bit or 10-byte floats. AFAIK Java doesn't support them
     */
    public double get() {

        if (data.limit() == 4)
            return data.duplicate().getFloat();
        

        if (data.limit() == 8) 
            return data.duplicate().getDouble();

        throw new ArithmeticException("80-bit floats are not supported");
    }

}