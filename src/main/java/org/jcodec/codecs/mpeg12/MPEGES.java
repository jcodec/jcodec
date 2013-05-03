package org.jcodec.codecs.mpeg12;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.containers.mps.MPSDemuxer.MPEGPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Pulls frames from MPEG elementary stream
 * 
 * @author The JCodec project
 * 
 */
public class MPEGES extends SegmentReader {

    private int frameNo;
    public long curPts;

    public MPEGES(ReadableByteChannel channel) throws IOException {
        super(channel, 4096);
    }

    public MPEGES(ReadableByteChannel channel, int fetchSize) throws IOException {
        super(channel, fetchSize);
    }

    public MPEGPacket getFrame(ByteBuffer buffer) throws IOException {

        ByteBuffer dup = buffer.duplicate();

        while (curMarker != 0x100 && curMarker != 0x1b3 && skipToMarker())
            ;

        while (curMarker != 0x100 && readToNextMarker(dup))
            ;

        readToNextMarker(dup);

        while (curMarker != 0x100 && curMarker != 0x1b3 && readToNextMarker(dup))
            ;

        dup.flip();

        return dup.hasRemaining() ? new MPEGPacket(dup, curPts, 90000, 0, frameNo++, true, null) : null;
    }
}