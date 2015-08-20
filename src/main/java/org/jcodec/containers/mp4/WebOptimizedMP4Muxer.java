package org.jcodec.containers.mp4;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

public class WebOptimizedMP4Muxer extends MP4Muxer {
    private ByteBuffer header;
    private long headerPos;

    public static WebOptimizedMP4Muxer withOldHeader(SeekableByteChannel output, Brand brand, MovieBox oldHeader)
            throws IOException {
        int size = (int) oldHeader.getHeader().getSize();
        TrakBox vt = oldHeader.getVideoTrack();

        SampleToChunkBox stsc = vt.getStsc();
        size -= stsc.getSampleToChunk().length * 12;
        size += 12;

        ChunkOffsetsBox stco = vt.getStco();
        if (stco != null) {
            size -= stco.getChunkOffsets().length << 2;
            size += vt.getFrameCount() << 3;
        } else {
            ChunkOffsets64Box co64 = vt.getCo64();
            size -= co64.getChunkOffsets().length << 3;
            size += vt.getFrameCount() << 3;
        }

        return new WebOptimizedMP4Muxer(output, brand, size + (size >> 1));
    }

    public WebOptimizedMP4Muxer(SeekableByteChannel output, Brand brand, int headerSize) throws IOException {
        super(output, brand);
        headerPos = output.position() - 24;
        output.position(headerPos);

        header = ByteBuffer.allocate(headerSize);
        output.write(header);
        header.clear();

        new Header("wide", 8).write(output);
        new Header("mdat", 1).write(output);
        mdatOffset = output.position();
        NIOUtils.writeLong(output, 0);
    }

    @Override
    public void storeHeader(MovieBox movie) throws IOException {
        long mdatEnd = out.position();
        long mdatSize = mdatEnd - mdatOffset + 8;
        out.position(mdatOffset);
        NIOUtils.writeLong(out, mdatSize);

        out.position(headerPos);
        try {
            movie.write(header);
            header.flip();
            int rem = header.capacity() - header.limit();
            if (rem < 8) {
                header.duplicate().putInt(header.capacity());
            }
            out.write(header);
            if (rem >= 8)
                new Header("free", rem).write(out);
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.warn("Could not web-optimize, header is bigger then allocated space.");
            new Header("free", header.remaining()).write(out);
            out.position(mdatEnd);
            MP4Util.writeMovie(out, movie);
        }
    }
}