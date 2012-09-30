package org.jcodec.common.io;

import java.io.DataOutput;
import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class WriterBE {

    public static void writePascalString(DataOutput out, String val, int len) throws IOException {
        byte[] res = new byte[len];
        System.arraycopy(val.getBytes(), 0, res, 0, val.length());
        out.write(val.length());
        out.write(res);
    }

    public static byte[] intBytes(int code) {
        return new byte[] { (byte) (code >>> 24), (byte) ((code >> 16) & 0xff), (byte) ((code >> 8) & 0xff),
                (byte) (code & 0xff) };
    }

    public static void writeLong(RandomAccessFileOutputStream ros, long pts) {
        // TODO Auto-generated method stub
        
    }

    public static void writeInt(RandomAccessFileOutputStream ros, long duration) {
        // TODO Auto-generated method stub
        
    }

    public static void writeByte(RandomAccessFileOutputStream ros, int i) {
        // TODO Auto-generated method stub
        
    }
}
