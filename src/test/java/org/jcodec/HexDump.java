/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jcodec;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Dumps data in hexadecimal format.
 * <p>
 * Provides a single function to take an array of bytes and display it in
 * hexadecimal form.
 * <p>
 * Origin of code: POI.
 *
 * @version $Id: HexDump.java 1302748 2012-03-20 01:35:32Z ggregory $
 */
public class HexDump {

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public HexDump() {
        super();
    }

    /**
     * Dump an array of bytes to an OutputStream. The output is formatted for
     * human inspection, with a hexadecimal offset followed by the hexadecimal
     * values of the next 16 bytes of data and the printable ASCII characters
     * (if any) that those bytes represent printed per each line of output.
     * <p>
     * The offset argument specifies the start offset of the data array within a
     * larger entity like a file or an incoming stream. For example, if the data
     * array contains the third kibibyte of a file, then the offset argument
     * should be set to 2048. The offset value printed at the beginning of each
     * line indicates where in that larger entity the first byte on that line is
     * located.
     * <p>
     * All bytes between the given index (inclusive) and the end of the data
     * array are dumped.
     *
     * @param data
     *            the byte array to be dumped
     * @param offset
     *            offset of the byte array within a larger entity
     * @param stream
     *            the OutputStream to which the data is to be written
     * @param index
     *            initial index into the byte array
     *
     * @throws IOException
     *             is thrown if anything goes wrong writing the data to stream
     * @throws ArrayIndexOutOfBoundsException
     *             if the index is outside the data array's bounds
     * @throws IllegalArgumentException
     *             if the output stream is null
     */

    public static void dumpOut(byte[] data, long offset, OutputStream stream, int index, int len) throws IOException,
            ArrayIndexOutOfBoundsException, IllegalArgumentException {

        if (index < 0 || index >= data.length) {
            throw new ArrayIndexOutOfBoundsException("illegal index: " + index + " into array of length " + data.length);
        }
        if (stream == null) {
            throw new IllegalArgumentException("cannot write to nullstream");
        }
        long display_offset = offset + index;
        StringBuilder buffer = new StringBuilder(74);

        for (int j = index; j < len; j += 16) {
            int chars_read = len - j;

            if (chars_read > 16) {
                chars_read = 16;
            }
            dumpLong(buffer, display_offset).append(' ');
            for (int k = 0; k < 16; k++) {
                if (k < chars_read) {
                    dumpByte(buffer, data[k + j]);
                } else {
                    buffer.append("  ");
                }
                buffer.append(' ');
            }
            for (int k = 0; k < chars_read; k++) {
                if (data[k + j] >= ' ' && data[k + j] < 127) {
                    buffer.append((char) data[k + j]);
                } else {
                    buffer.append('.');
                }
            }
            buffer.append(EOL);
            stream.write(buffer.toString().getBytes());
            stream.flush();
            buffer.setLength(0);
            display_offset += chars_read;
        }
    }

    public static String hexdump(ByteBuffer data) {
        StringBuilder sb = new StringBuilder();
        dump(data, 0, sb);
        return sb.toString();
    }

    public static String hexdump0(ByteBuffer data) {
        StringBuilder sb = new StringBuilder();
        dump(data, -data.position(), sb);
        return sb.toString();
    }

    public static StringBuilder dump(ByteBuffer data, long offset, StringBuilder buffer) {
        int index = data.position();
        int len = data.limit();
        long display_offset = offset + index;
        for (int j = index; j < len; j += 16) {
            int chars_read = len - j;

            if (chars_read > 16) {
                chars_read = 16;
            }
            dumpLong(buffer, display_offset).append(' ');
            for (int k = 0; k < 16; k++) {
                if (k < chars_read) {
                    dumpByte(buffer, data.get(k + j));
                } else {
                    buffer.append("  ");
                }
                buffer.append(' ');
            }
            for (int k = 0; k < chars_read; k++) {
                byte b = data.get(k + j);
                if (b >= ' ' && b < 127) {
                    buffer.append((char) b);
                } else {
                    buffer.append('.');
                }
            }
            buffer.append(EOL);
            display_offset += chars_read;
        }
        return buffer;
    }

    /**
     * The line-separator (initializes to "line.separator" system property.
     */
    public static final String EOL = System.getProperty("line.separator");
    private static final char[] _hexcodes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F' };
    private static final int[] _shifts = { 28, 24, 20, 16, 12, 8, 4, 0 };

    /**
     * Dump a long value into a StringBuilder.
     *
     * @param _lbuffer
     *            the StringBuilder to dump the value in
     * @param value
     *            the long value to be dumped
     * @return StringBuilder containing the dumped value.
     */
    private static StringBuilder dumpLong(StringBuilder _lbuffer, long value) {
        for (int j = 0; j < 8; j++) {
            _lbuffer.append(_hexcodes[(int) (value >> _shifts[j]) & 15]);
        }
        return _lbuffer;
    }

    /**
     * Dump a byte value into a StringBuilder.
     *
     * @param _cbuffer
     *            the StringBuilder to dump the value in
     * @param value
     *            the byte value to be dumped
     * @return StringBuilder containing the dumped value.
     */
    private static StringBuilder dumpByte(StringBuilder _cbuffer, byte value) {
        for (int j = 0; j < 2; j++) {
            _cbuffer.append(_hexcodes[value >> _shifts[j + 6] & 15]);
        }
        return _cbuffer;
    }

}
