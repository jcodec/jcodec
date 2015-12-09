package org.jcodec.codecs.mpeg12;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Pulls frames from MPEG elementary stream
 * 
 * @author The JCodec project
 * 
 */
public class SegmentReader {

    private ReadableByteChannel channel;
    private ByteBuffer buf;
    protected int curMarker;
    private int fetchSize;
    private boolean done;
    private long pos;

    public SegmentReader(ReadableByteChannel channel) throws IOException {
        this(channel, 4096);
    }

    public SegmentReader(ReadableByteChannel channel, int fetchSize) throws IOException {
        this.channel = channel;
        this.fetchSize = fetchSize;
        buf = NIOUtils.fetchFrom(channel, 4);
        pos = buf.remaining();
        curMarker = buf.getInt();
    }

    public final boolean readToNextMarker(ByteBuffer out) throws IOException {
        if (done)
            return false;
        int n = 1;
        do {
            while (buf.hasRemaining()) {
                if (curMarker >= 0x100 && curMarker <= 0x1ff) {
                    if (n == 0) {
                        return true;
                    }
                    --n;
                }
                out.put((byte) (curMarker >>> 24));
                curMarker = (curMarker << 8) | (buf.get() & 0xff);
            }
            buf = NIOUtils.fetchFrom(channel, fetchSize);
            pos += buf.remaining();
        } while (buf.hasRemaining());
        out.putInt(curMarker);
        done = true;

        return false;
    }

    public final boolean skipToMarker() throws IOException {
        if (done)
            return false;
        do {
            while (buf.hasRemaining()) {
                curMarker = (curMarker << 8) | (buf.get() & 0xff);
                if (curMarker >= 0x100 && curMarker <= 0x1ff) {
                    return true;
                }
            }
            buf = NIOUtils.fetchFrom(channel, fetchSize);
            pos += buf.remaining();
        } while (buf.hasRemaining());
        done = true;

        return false;
    }

    public final boolean read(ByteBuffer out, int length) throws IOException {
        if (done)
            return false;
        do {
            while (buf.hasRemaining()) {
                if (length-- == 0)
                    return true;
                out.put((byte) (curMarker >>> 24));
                curMarker = (curMarker << 8) | (buf.get() & 0xff);
            }
            buf = NIOUtils.fetchFrom(channel, fetchSize);
            pos += buf.remaining();
        } while (buf.hasRemaining());
        out.putInt(curMarker);
        done = true;

        return false;
    }

    public final long curPos() {
        return pos - buf.remaining() - 4;
    }
}