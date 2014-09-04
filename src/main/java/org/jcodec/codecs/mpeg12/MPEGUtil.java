package org.jcodec.codecs.mpeg12;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEGUtil {

    /**
     * Finds next MPEG bitstream marker 0x000001xx and returns the data that
     * preceeds it as a ByteBuffer slice
     * 
     * Segment byte order is always little endian
     * 
     * @param buf
     * @return
     */
    public static ByteBuffer gotoNextMarker(ByteBuffer buf) {
        return gotoMarker(buf, 0, 0x100, 0x1ff);
    }

    /**
     * Finds next Nth MPEG bitstream marker 0x000001xx and returns the data that
     * preceeds it as a ByteBuffer slice
     * 
     * Segment byte order is always little endian
     * 
     * @param buf
     * @return
     */
    public static ByteBuffer gotoMarker(ByteBuffer buf, int n, int mmin, int mmax) {
        if (!buf.hasRemaining())
            return null;

        int from = buf.position();
        ByteBuffer result = buf.slice();
        result.order(ByteOrder.BIG_ENDIAN);

        int val = 0xffffffff;
        while (buf.hasRemaining()) {
            val = (val << 8) | (buf.get() & 0xff);
            if (val >= mmin && val <= mmax) {
                if (n == 0) {
                    buf.position(buf.position() - 4);
                    result.limit(buf.position() - from);
                    break;
                }
                --n;
            }
        }
        return result;
    }

    /**
     * Returns next segment between two MPEG marker
     * 
     * i.e. searches for the next marker if the stream is not at the marker
     * boundary already
     * 
     * @param buf
     * @return
     */
    public static ByteBuffer nextSegment(ByteBuffer buf) {
        gotoMarker(buf, 0, 0x100, 0x1ff);
        return gotoMarker(buf, 1, 0x100, 0x1ff);
    }
}
