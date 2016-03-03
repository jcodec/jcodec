package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class StringReader {
    public static String readString(InputStream input, int len) throws IOException {
        byte[] bs = _sureRead(input, len);
        return bs == null ? null : Platform.stringFromBytes(bs);
    }

    public static byte[] _sureRead(InputStream input, int len) throws IOException {
        byte[] res = new byte[len];
        if (sureRead(input, res, res.length) == len)
            return res;
        return null;
    }

    public static int sureRead(InputStream input, byte[] buf, int len) throws IOException {
        int read = 0;
        while (read < len) {
            int tmp = input.read(buf, read, len - read);
            if (tmp == -1)
                break;
            read += tmp;
        }
        return read;
    }

    public static void sureSkip(InputStream is, long l) throws IOException {
        while (l > 0)
            l -= is.skip(l);
    }
}
