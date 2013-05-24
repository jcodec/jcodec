package org.jcodec.containers.mp4;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.RAOutputStream;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Tries to create a web-optimized movie
 * 
 * @author The JCodec project
 * 
 */
public class WebOptimizedMP4Muxer extends MP4Muxer {

    private byte[] header;
    private long headerPos;

    public static WebOptimizedMP4Muxer withOldHeader(RAOutputStream output, Brand brand, MovieBox oldHeader)
            throws IOException {
        int size = (int) oldHeader.getHeader().getSize();
        TrakBox vt = oldHeader.getVideoTrack();

        SampleToChunkBox stsc = Box.findFirst(vt, SampleToChunkBox.class, "mdia", "minf", "stbl", "stsc");
        size -= stsc.getSampleToChunk().length * 12;
        size += 12;

        ChunkOffsetsBox stco = Box.findFirst(vt, ChunkOffsetsBox.class, "mdia", "minf", "stbl", "stco");
        if (stco != null) {
            size -= stco.getChunkOffsets().length << 2;
            size += vt.getFrameCount() << 3;
        } else {
            ChunkOffsets64Box co64 = Box.findFirst(vt, ChunkOffsets64Box.class, "mdia", "minf", "stbl", "co64");
            size -= co64.getChunkOffsets().length << 3;
            size += vt.getFrameCount() << 3;
        }

        return new WebOptimizedMP4Muxer(output, brand, size + (size >> 1));
    }

    public WebOptimizedMP4Muxer(RAOutputStream output, Brand brand, int headerSize) throws IOException {
        super(output, brand);
        headerPos = output.getPos() - 16;
        output.seek(headerPos);

        header = new byte[headerSize];
        output.write(header);

        new Header("mdat", 1).write(output);
        mdatOffset = output.getPos();
        output.writeLong(0);
    }

    @Override
    public void storeHeader(MovieBox movie) throws IOException {
        long mdatEnd = out.getPos();
        long mdatSize = mdatEnd - mdatOffset + 8;
        out.seek(mdatOffset);
        out.writeLong(mdatSize);

        out.seek(headerPos);
        try {
            int len = movie.write(header);
            int rem = header.length - len;
            if (rem < 8) {
                ByteBuffer.wrap(header).asIntBuffer().put(header.length);
            }
            out.write(header, 0, len);
            if (rem >= 8)
                new Header("free", rem).write(out);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Could not web-optimize, header is bigger then allocated space.");
            new Header("free", header.length).write(out);
            out.seek(mdatEnd);
            movie.write(out);
        }
    }
}
