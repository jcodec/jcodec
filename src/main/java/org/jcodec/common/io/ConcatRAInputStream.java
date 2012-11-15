package org.jcodec.common.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Provides random access to a set of concatenated resources represented by
 * random access input streams
 * 
 * @author The JCodec project
 * 
 */
public class ConcatRAInputStream extends RAInputStream {

    private RAInputStream[] streams;
    private long[] offsets;
    private int cur;
    private long total;

    public ConcatRAInputStream(RAInputStream... _streams) throws IOException {
        this(Arrays.asList(_streams));
    }

    public ConcatRAInputStream(List<RAInputStream> _streams) throws IOException {
        if (_streams.size() == 0)
            throw new IllegalArgumentException("Empty set of streams");
        streams = _streams.toArray(new RAInputStream[0]);
        offsets = new long[this.streams.length];
        total = 0;
        for (int i = 0; i < streams.length; i++) {
            long len = streams[i].length();
            offsets[i] = total;
            total += len;
        }
    }

    @Override
    public void seek(long where) throws IOException {
        if (where > total)
            return;
        if (where < 0)
            return;
        int i;
        for (i = 0; i < offsets.length; i++) {
            if (where < offsets[i])
                break;
        }
        cur = i - 1;
        streams[cur].seek(where - offsets[cur]);
    }

    @Override
    public long getPos() throws IOException {
        return offsets[cur] + streams[cur].getPos();
    }

    @Override
    public long length() throws IOException {
        return total;
    }

    @Override
    public int read() throws IOException {
        int res = streams[cur].read();
        if (res == -1 && cur < streams.length - 1) {
            cur++;
            streams[cur].seek(0);
            res = streams[cur].read();
        }

        return res;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int rem = len - streams[cur].read(b, off, len);
        while (rem > 0 && cur < streams.length - 1) {
            cur++;
            streams[cur].seek(0);
            rem -= streams[cur].read(b, off, rem);
        }
        return len - rem;
    }
}
