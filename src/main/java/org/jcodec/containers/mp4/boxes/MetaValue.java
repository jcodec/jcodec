package org.jcodec.containers.mp4.boxes;

import org.jcodec.platform.Platform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MetaValue {
    // All data types below are Big Endian
    public static final int TYPE_STRING_UTF16 = 2;
    public static final int TYPE_STRING_UTF8 = 1;
    public static final int TYPE_FLOAT_64 = 24;
    public static final int TYPE_FLOAT_32 = 23;
    public static final int TYPE_INT_32 = 67;
    public static final int TYPE_INT_16 = 66;
    public static final int TYPE_INT_8 = 65;
    public static final int TYPE_INT_V = 22;
    public static final int TYPE_UINT_V = 21;
    public static final int TYPE_JPEG = 13;
    public static final int TYPE_PNG = 13;
    public static final int TYPE_BMP = 27;
    
    private int type;
    private int locale;
    private byte[] data;

    private MetaValue(int type, int locale, byte[] data) {
        this.type = type;
        this.locale = locale;
        this.data = data;
    }

    public static MetaValue createInt(int value) {
        return new MetaValue(21, 0, fromInt(value));
    }

    public static MetaValue createFloat(float value) {
        return new MetaValue(23, 0, fromFloat(value));
    }

    public static MetaValue createString(String value) {
        return new MetaValue(1, 0, Platform.getBytesForCharset(value, Platform.UTF_8));
    }

    public static MetaValue createOther(int type, byte[] data) {
        return new MetaValue(type, 0, data);
    }

    public static MetaValue createOtherWithLocale(int type, int locale, byte[] data) {
        return new MetaValue(type, locale, data);
    }

    public int getInt() {
        if (type == TYPE_UINT_V || type == TYPE_INT_V) {
            switch (data.length) {
            case 1:
                return data[0];
            case 2:
                return toInt16(data);
            case 3:
                return toInt24(data);
            case 4:
                return toInt32(data);
            }
        }
        if (type == TYPE_INT_8)
            return data[0];
        if (type == TYPE_INT_16)
            return toInt16(data);
        if (type == TYPE_INT_32)
            return toInt32(data);
        return 0;
    }

    public double getFloat() {
        if (type == TYPE_FLOAT_32)
            return toFloat(data);
        if (type == TYPE_FLOAT_64)
            return toDouble(data);
        return 0;
    }

    public String getString() {
        if (type == TYPE_STRING_UTF8)
            return Platform.stringFromCharset(data, Platform.UTF_8);
        if (type == TYPE_STRING_UTF16) {
            return Platform.stringFromCharset(data, Platform.UTF_16BE);
        }
        return null;
    }
    
    public boolean isInt() {
        return type == TYPE_UINT_V || type == TYPE_INT_V || type == TYPE_INT_8 || type == TYPE_INT_16 || type == TYPE_INT_32;
    }

    public boolean isString() {
        return type == TYPE_STRING_UTF8 || type == TYPE_STRING_UTF16;
    }

    public boolean isFloat() {
        return type == TYPE_FLOAT_32 || type == TYPE_FLOAT_64;
    }

    public String toString() {
        if (isInt())
            return String.valueOf(getInt());
        else if (isFloat())
            return String.valueOf(getFloat());
        else if (isString())
            return String.valueOf(getString());
        else
            return "BLOB";
    }

    public int getType() {
        return type;
    }

    public int getLocale() {
        return locale;
    }

    public byte[] getData() {
        return data;
    }

    private static byte[] fromFloat(float floatValue) {
        byte[] bytes = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putFloat(floatValue);
        return bytes;
    }

    private static byte[] fromInt(int value) {
        byte[] bytes = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(value);
        return bytes;
    }

    private int toInt16(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort();
    }

    private int toInt24(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return ((bb.getShort() & 0xffff) << 8) | (bb.get() & 0xff);
    }

    private int toInt32(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();
    }
    
    private float toFloat(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getFloat();
    }

    private double toDouble(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getDouble();
    }

    public boolean isBlob() {
        return !isFloat() && !isInt() && !isString();
    }
}