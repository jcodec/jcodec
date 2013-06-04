package org.jcodec.movtool.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.PriorityCallable;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Retrieves chunks for the range concurrently allowing for concurrent
 * transcode-on-the-fly
 * 
 * @author The JCodec project
 * 
 */
public class ConcurrentMovieService {
    private ExecutorService exec;
    private VirtualMovie movie;

    public ConcurrentMovieService(VirtualMovie movie, int nThreads) {
        this.exec = JCodecUtil.getPriorityExecutor(nThreads);
        this.movie = movie;
    }

    public InputStream getRange(long from, long to) {
        return new ConcurrentMovieRange(exec, movie, from, to);
    }

    static class GetCallable implements PriorityCallable<ByteBuffer> {
        private MovieSegment chunk;
        private int priority;

        public GetCallable(MovieSegment chunk, int priority) {
            this.chunk = chunk;
            this.priority = priority;
        }

        public ByteBuffer call() throws Exception {
            return chunk.getData().duplicate();
        }

        public int getPriority() {
            return priority;
        }
    }

    public static class ConcurrentMovieRange extends InputStream {
        private List<Future<ByteBuffer>> chunks;
        private VirtualMovie movie;

        public ConcurrentMovieRange(ExecutorService exec, VirtualMovie movie, long from, long to) {
            this.movie = movie;
            movie.take();
            MovieSegment chunk = movie.getPacketAt(from);
            int initial = chunk.getNo();
            do {
                Future<ByteBuffer> submit = exec.submit(new GetCallable(chunk, chunk.getNo() - initial));
                chunks.add(submit);
                chunk = movie.getPacketByNo(chunk.getNo());
            } while (chunk.getPos() < to);
        }

        @Override
        public int read(byte[] b, int from, int len) throws IOException {
            if (chunks.size() == 0)
                return -1;

            int totalRead = 0;
            while (len > 0 && chunks.size() > 0) {
                ByteBuffer chunkData = chunkData();
                int toRead = Math.min(chunkData.remaining(), len);

                chunkData.get(b, from, toRead);
                totalRead += toRead;
                len -= toRead;
                from += toRead;

                if (!chunkData.hasRemaining())
                    chunks.remove(0);
            }

            return totalRead;
        }

        private ByteBuffer chunkData() throws IOException {
            ByteBuffer chunkData;
            try {
                chunkData = chunks.get(0).get();
            } catch (InterruptedException e) {
                throw new IOException(e);
            } catch (ExecutionException e) {
                throw new IOException(e);
            }
            return chunkData;
        }

        @Override
        public void close() throws IOException {
            for (Future<ByteBuffer> future : chunks) {
                future.cancel(false);
            }
            movie.release();
        }

        @Override
        public int read() throws IOException {
            if (chunks.size() == 0)
                return -1;

            ByteBuffer chunkData = chunkData();
            int ret = chunkData.get() & 0xff;

            if (!chunkData.hasRemaining())
                chunks.remove(0);

            return ret;
        }
    }
}