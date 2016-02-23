package org.jcodec.movtool.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.platform.BaseInputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Retrieves a movie range
 * 
 * @author The JCodec project
 * 
 */
public class MovieRange extends BaseInputStream {
    private VirtualMovie movie;
    private long remaining;
    private int chunkNo;
    private ByteBuffer chunkData;

    public MovieRange(VirtualMovie movie, long from, long to) throws IOException {
        if (to < from)
            throw new IllegalArgumentException("from < to");
        this.movie = movie;
        MovieSegment chunk = movie.getPacketAt(from);
        this.remaining = to - from + 1;
        if (chunk != null) {
            chunkData = checkDataLen(chunk.getData(), chunk.getDataLen());
            chunkNo = chunk.getNo();
            NIOUtils.skip(chunkData, (int) (from - chunk.getPos()));
        }
    }

    static ByteBuffer checkDataLen(ByteBuffer chunkData, int chunkDataLen) throws IOException {
        if(chunkData == null) {
            System.err.println("WARN: packet expected data len != actual data len " + chunkDataLen + " != 0"
                    );
            return ByteBuffer.allocate(chunkDataLen);
        }
        if (chunkData.remaining() != chunkDataLen) {
            System.err.println("WARN: packet expected data len != actual data len " + chunkDataLen + " != "
                    + chunkData.remaining());
            chunkDataLen = Math.max(0, chunkDataLen);

            if (chunkDataLen < chunkData.remaining() || chunkData.capacity() - chunkData.position() >= chunkDataLen) {
                chunkData.limit(chunkData.position() + chunkDataLen);
            } else {
                ByteBuffer correct = ByteBuffer.allocate(chunkDataLen);
                correct.put(chunkData);
                correct.clear();
                return correct;
            }
        }
        return chunkData;
    }

    @Override
    public int readBuffer(byte[] b, int from, int len) throws IOException {
        tryFetch();
        if (chunkData == null || remaining == 0)
            return -1;

        len = (int) Math.min(remaining, len);
        int totalRead = 0;
        while (len > 0) {
            int toRead = Math.min(chunkData.remaining(), len);

            chunkData.get(b, from, toRead);
            totalRead += toRead;
            len -= toRead;
            from += toRead;

            tryFetch();
            if (chunkData == null)
                break;
        }
        remaining -= totalRead;

        return totalRead;
    }

    private void tryFetch() throws IOException {
        if (chunkData == null || !chunkData.hasRemaining()) {
            MovieSegment chunk = movie.getPacketByNo(chunkNo + 1);
            if (chunk != null) {
                chunkData = checkDataLen(chunk.getData(), chunk.getDataLen());
                chunkNo = chunk.getNo();
            } else
                chunkData = null;
        }
    }

    @Override
    public int readByte() throws IOException {
        tryFetch();
        if (chunkData == null || remaining == 0)
            return -1;

        --remaining;

        return chunkData.get() & 0xff;
    }
}