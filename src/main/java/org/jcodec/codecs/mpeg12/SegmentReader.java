package org.jcodec.codecs.mpeg12;
import org.jcodec.common.io.NIOUtils;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

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
    protected boolean done;
    private long pos;
    private int bytesInMarker;
    private int bufferIncrement = 1 << 15; // 32k

    public SegmentReader(ReadableByteChannel channel, int fetchSize) throws IOException {
        this.channel = channel;
        this.fetchSize = fetchSize;
        buf = NIOUtils.fetchFromChannel(channel, 4);
        pos = buf.remaining();
        curMarker = buf.getInt();
        bytesInMarker = 4;
    }
    
    
    
    // FOR TESTCASES
    public int getBufferIncrement() {
        return bufferIncrement;
    }

    // FOR TESTCASES
    public void setBufferIncrement(int bufferIncrement) {
        this.bufferIncrement = bufferIncrement;
    }



    public static enum State {
        MORE_DATA, DONE, STOP
    }
    
    /**
     * Reads one full segment till the next marker. Will read as much data as
     * the provided buffer fits, if the provided buffer doesn't fit all data
     * will return MORE_DATA.
     * 
     * @param out
     * @return
     * @throws IOException
     */
    public final State readToNextMarkerPartial(ByteBuffer out) throws IOException {
        if (done)
            return State.STOP;
        int skipOneMarker = curMarker >= 0x100 && curMarker <= 0x1ff ? 1 : 0;
        int written = out.position();
        do {
            while (buf.hasRemaining()) {
                if (curMarker >= 0x100 && curMarker <= 0x1ff) {
                    if (skipOneMarker == 0) {
                        return State.DONE;
                    }
                    --skipOneMarker;
                }
                if (!out.hasRemaining())
                    return State.MORE_DATA;
                out.put((byte) (curMarker >>> 24));
                curMarker = (curMarker << 8) | (buf.get() & 0xff);
            }
            buf = NIOUtils.fetchFromChannel(channel, fetchSize);
            pos += buf.remaining();
        } while (buf.hasRemaining());

        written = out.position() - written;
        if (written > 0 && curMarker >= 0x100 && curMarker <= 0x1ff)
            return State.DONE;

        for (; bytesInMarker > 0 && out.hasRemaining();) {
            out.put((byte) (curMarker >>> 24));
            curMarker = (curMarker << 8);
            --bytesInMarker;
            if (curMarker >= 0x100 && curMarker <= 0x1ff)
                return State.DONE;
        }

        if (bytesInMarker == 0) {
            done = true;
            return State.STOP;
        } else {
            return State.MORE_DATA;
        }
    }
    
        
    /**
     * Reads one full segment till the next marker. Will allocate the necessary
     * buffer to hold the full segment. Internally uses a growing collection of
     * smaller buffers since the segment size is intitially unkwnown.
     * 
     * @return
     * @throws IOException
     */
    public ByteBuffer readToNextMarkerNewBuffer() throws IOException {
        if (done)
            return null;
        List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
        readToNextMarkerBuffers(buffers);

        return NIOUtils.combineBuffers(buffers);
    }


    public void readToNextMarkerBuffers(List<ByteBuffer> buffers) throws IOException {
        State state;
        do {
            ByteBuffer curBuffer = ByteBuffer.allocate(bufferIncrement);
            state = readToNextMarkerPartial(curBuffer);
            curBuffer.flip();
            buffers.add(curBuffer);
        } while (state == State.MORE_DATA);
    }

    public final boolean readToNextMarker(ByteBuffer out) throws IOException {
        State state = readToNextMarkerPartial(out);
        if(state == State.MORE_DATA)
            throw new BufferOverflowException();
        return state == State.DONE;
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
            buf = NIOUtils.fetchFromChannel(channel, fetchSize);
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
            buf = NIOUtils.fetchFromChannel(channel, fetchSize);
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