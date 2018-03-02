package org.jcodec.codecs.mpeg12;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.containers.mps.MPEGPacket;

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
    public long lastKnownDuration;

    public MPEGES(ReadableByteChannel channel, int fetchSize) throws IOException {
        super(channel, fetchSize);
    }

    /**
     * Reads one MPEG1/2 video frame from MPEG1/2 elementary stream into a
     * provided buffer.
     * 
     * @param buffer
     *            A buffer to use for the data.
     * @return A packet with a video frame or null at for end of the stream. The
     *         data buffer inside the packet will be a sub-buffer of a 'buffer'
     *         provided as an argument.
     * @throws IOException
     */
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
        
        PictureHeader ph = MPEGDecoder.getPictureHeader(dup.duplicate());

        return dup.hasRemaining() ? new MPEGPacket(dup, 0, 90000, 0, frameNo++,
                ph.picture_coding_type <= MPEGConst.IntraCoded ? FrameType.KEY : FrameType.INTER, null) : null;
    }

    /**
     * Reads one MPEG1/2 video frame from MPEG1/2 elementary stream.
     * 
     * @return A packet with a video frame or null at the end of stream.
     * @throws IOException
     */
    public MPEGPacket getFrame() throws IOException {

        while (curMarker != 0x100 && curMarker != 0x1b3 && skipToMarker())
            ;

        List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
        // Reading to the frame header, sequence header, sequence header
        // extensions and group header go in here
        while (curMarker != 0x100 && !done)
            readToNextMarkerBuffers(buffers);

        // Reading the frame header
        readToNextMarkerBuffers(buffers);

        // Reading the slices, will stop on encounter of a frame header of the
        // next frame or a sequence header
        while (curMarker != 0x100 && curMarker != 0x1b3 && !done)
            readToNextMarkerBuffers(buffers);

        ByteBuffer dup = NIOUtils.combineBuffers(buffers);
        PictureHeader ph = MPEGDecoder.getPictureHeader(dup.duplicate());
        
        return dup.hasRemaining() ? new MPEGPacket(dup, 0, 90000, 0, frameNo++,
                ph.picture_coding_type <= MPEGConst.IntraCoded ? FrameType.KEY : FrameType.INTER, null) : null;
    }
}