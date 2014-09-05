package org.jcodec.containers.mps.index;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jcodec.common.RunLength;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents index for MPEG PS stream, enables demuxers to do precise seek
 * 
 * @author The JCodec project
 * 
 */
public class MPSIndex {
    private long[] pesTokens;
    private RunLength.Integer pesStreamIds;
    private MPSStreamIndex[] streams;

    public static class MPSStreamIndex {
        protected int streamId;
        protected int siLen;
        protected int[] fsizes;
        protected int[] fpts;
        protected int[] sync;

        public MPSStreamIndex(int streamId, int siLen, int[] fsizes, int[] fpts, int[] sync) {
            this.streamId = streamId;
            this.siLen = siLen;
            this.fsizes = fsizes;
            this.fpts = fpts;
            this.sync = sync;
        }

        public MPSStreamIndex(MPSStreamIndex other) {
            this(other.streamId, other.siLen, other.fsizes, other.fpts, other.sync);
        }

        public int getStreamId() {
            return streamId;
        }

        public int getSiLen() {
            return siLen;
        }

        public int[] getFsizes() {
            return fsizes;
        }

        public int[] getFpts() {
            return fpts;
        }

        public int[] getSync() {
            return sync;
        }

        public static MPSStreamIndex parseIndex(int streamId, ByteBuffer index) {
            int siLen = index.getInt();

            int fCnt = index.getInt();
            int[] fsizes = new int[fCnt];
            int[] fpts = new int[fCnt];
            for (int i = 0; i < fCnt; i++) {
                int size = index.getInt();
                fsizes[i] = size;
            }

            int syncCount = index.getInt();
            int[] sync = new int[syncCount];
            for (int i = 0; i < syncCount; i++)
                sync[i] = index.getInt();

            for (int i = 0; i < fCnt; i++) {
                fpts[i] = index.getInt();
            }

            return new MPSStreamIndex(streamId, siLen, fsizes, fpts, sync);
        }

        public void serialize(ByteBuffer index) {
            index.put((byte) streamId);
            index.putInt(fsizes.length);
            for (int i = 0; i < fsizes.length; i++)
                index.putInt(fsizes[i]);
            index.putInt(sync.length);
            for (int i = 0; i < sync.length; i++)
                index.putInt(sync[i]);
            for (int i = 0; i < fpts.length; i++)
                index.putInt(fpts[i]);
        }
    }

    public MPSIndex(long[] pesTokens, RunLength.Integer pesStreamIds, MPSStreamIndex[] streams) {
        this.pesTokens = pesTokens;
        this.pesStreamIds = pesStreamIds;
        this.streams = streams;
    }

    public MPSIndex(MPSIndex mpsIndex) {
        this(mpsIndex.pesTokens, mpsIndex.pesStreamIds, mpsIndex.streams);
    }

    public long[] getPesTokens() {
        return pesTokens;
    }

    public RunLength.Integer getPesStreamIds() {
        return pesStreamIds;
    }

    public MPSStreamIndex[] getStreams() {
        return streams;
    }

    public static MPSIndex parseIndex(ByteBuffer index) {
        int pesCnt = index.getInt();
        long[] pesTokens = new long[pesCnt];

        for (int i = 0; i < pesCnt; i++) {
            pesTokens[i] = index.getLong();
        }

        RunLength.Integer pesStreamId = RunLength.Integer.parse(index);

        ArrayList<MPSStreamIndex> list = new ArrayList<MPSStreamIndex>();
        while (index.hasRemaining()) {
            int stream = index.get() & 0xff;
            list.add(MPSStreamIndex.parseIndex(stream, index));
        }
        return new MPSIndex(pesTokens, pesStreamId, list.toArray(new MPSStreamIndex[0]));
    }

    public void serializeTo(ByteBuffer index) {
        index.putInt(pesTokens.length);
        for (int i = 0; i < pesTokens.length; i++) {
            index.putLong(pesTokens[i]);
        }
        pesStreamIds.serialize(index);

        MPSStreamIndex[] streams2 = streams;
        for (MPSStreamIndex mpsStreamIndex : streams2) {
            mpsStreamIndex.serialize(index);
        }
    }

    public static long makePESToken(long leading, long pesLen, long payloadLen) {
        return (leading << 48) | (pesLen << 24) | payloadLen;
    }

    public static int leadingSize(long token) {
        return (int) (token >> 48) & 0xffff;
    }

    public static int pesLen(long token) {
        return (int) (token >> 24) & 0xffffff;
    }

    public static int payLoadSize(long token) {
        return (int) token & 0xffffff;
    }
}