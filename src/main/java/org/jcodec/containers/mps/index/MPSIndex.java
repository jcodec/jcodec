package org.jcodec.containers.mps.index;
import org.jcodec.common.RunLength;

import js.nio.ByteBuffer;

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
    protected long[] pesTokens;
    protected RunLength.Integer pesStreamIds;
    protected MPSStreamIndex[] streams;

    public static class MPSStreamIndex {
        protected int streamId;
        protected int[] fsizes;
        protected int[] fpts;
        protected int[] fdur;
        protected int[] sync;

        public MPSStreamIndex(int streamId, int[] fsizes, int[] fpts, int[] fdur, int[] sync) {
            this.streamId = streamId;
            this.fsizes = fsizes;
            this.fpts = fpts;
            this.fdur = fdur;
            this.sync = sync;
        }

        public int getStreamId() {
            return streamId;
        }

        public int[] getFsizes() {
            return fsizes;
        }

        public int[] getFpts() {
            return fpts;
        }

        public int[] getFdur() {
            return fdur;
        }

        public int[] getSync() {
            return sync;
        }

        public static MPSStreamIndex parseIndex(ByteBuffer index) {
            int streamId = index.get() & 0xff;

            int fCnt = index.getInt();
            int[] fsizes = new int[fCnt];
            for (int i = 0; i < fCnt; i++) {
                fsizes[i] = index.getInt();
            }

            int fptsCnt = index.getInt();
            int[] fpts = new int[fptsCnt];
            for (int i = 0; i < fptsCnt; i++) {
                fpts[i] = index.getInt();
            }

            int fdurCnt = index.getInt();
            int[] fdur = new int[fdurCnt];
            for (int i = 0; i < fdurCnt; i++) {
                fdur[i] = index.getInt();
            }

            int syncCount = index.getInt();
            int[] sync = new int[syncCount];
            for (int i = 0; i < syncCount; i++)
                sync[i] = index.getInt();

            return new MPSStreamIndex(streamId, fsizes, fpts, fdur, sync);
        }

        public void serialize(ByteBuffer index) {
            index.put((byte) streamId);

            index.putInt(fsizes.length);
            for (int i = 0; i < fsizes.length; i++)
                index.putInt(fsizes[i]);

            index.putInt(fpts.length);
            for (int i = 0; i < fpts.length; i++)
                index.putInt(fpts[i]);

            index.putInt(fdur.length);
            for (int i = 0; i < fdur.length; i++)
                index.putInt(fdur[i]);

            index.putInt(sync.length);
            for (int i = 0; i < sync.length; i++)
                index.putInt(sync[i]);
        }

        public int estimateSize() {
            return (fpts.length << 2) + (fdur.length << 2) + (sync.length << 2) + (fsizes.length << 2) + 64;
        }
    }

    public MPSIndex(long[] pesTokens, RunLength.Integer pesStreamIds, MPSStreamIndex[] streams) {
        this.pesTokens = pesTokens;
        this.pesStreamIds = pesStreamIds;
        this.streams = streams;
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

        int nStreams = index.getInt();
        MPSStreamIndex[] streams = new MPSStreamIndex[nStreams];
        for (int i = 0; i < nStreams; i++) {
            streams[i] = MPSStreamIndex.parseIndex(index);
        }
        return new MPSIndex(pesTokens, pesStreamId, streams);
    }

    public void serializeTo(ByteBuffer index) {
        index.putInt(pesTokens.length);
        for (int i = 0; i < pesTokens.length; i++) {
            index.putLong(pesTokens[i]);
        }
        pesStreamIds.serialize(index);

        index.putInt(streams.length);
        for (MPSStreamIndex mpsStreamIndex : streams) {
            mpsStreamIndex.serialize(index);
        }
    }

    public int estimateSize() {
        int size = (pesTokens.length << 3) + pesStreamIds.estimateSize();
        for (MPSStreamIndex mpsStreamIndex : streams) {
            size += mpsStreamIndex.estimateSize();
        }
        return size + 64;
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