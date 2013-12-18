package org.jcodec.containers.mps;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer for MPEG Program Stream format with random access
 * 
 * Uses index to assist random access, see MPSIndexer
 * 
 * @author The JCodec project
 * 
 */
public class MPSRandomAccessDemuxer {

    private SeekableByteChannel ch;
    private List<Stream> streams;
    private int[] pesTokens;
    private short[] pesRLE;

    public MPSRandomAccessDemuxer(SeekableByteChannel ch, ByteBuffer index) throws IOException {
        this.ch = ch;
        streams = parseIndex(index);
    }

    private List<Stream> parseIndex(ByteBuffer index) throws IOException {
        Map<Integer, Stream> streams = new HashMap<Integer, Stream>();

        int pesCnt = index.getInt();
        pesTokens = new int[pesCnt];
        pesRLE = new short[pesCnt];

        for (int i = 0; i < pesCnt; i++) {
            byte b0 = index.get();
            pesRLE[i] = (short) ((b0 & 0x80) != 0 ? (((b0 & 0x7f) << 8) | (index.get() & 0xff)) + 1 : (b0 & 0xff) + 1);
            pesTokens[i] = index.getInt();
        }

        while (index.hasRemaining()) {
            int stream = index.get() & 0xff;
            getStream(streams, stream).parseIndex(index);
        }

        return new ArrayList<Stream>(streams.values());
    }

    public List<Stream> getStreams() {
        return streams;
    }

    private Stream getStream(Map<Integer, Stream> streams, int streamId) {
        Stream stream = streams.get(streamId);
        if (stream == null) {
            stream = new Stream(streamId);
            streams.put(streamId, stream);
        }
        return stream;
    }

    private class Stream implements SeekableDemuxerTrack {
        private int[] fsizes;
        private long[] foffs;
        private int curPesIdx;
        private int curFrame;
        private int siLen;
        private ByteBuffer si;
        private ByteBuffer pesBuf;
        private long curPesSubIdx;
        private int streamId;
        private long[] fpts;
        private int seekToFrame = -1;

        public Stream(int streamId) {
            this.streamId = streamId;
        }

        public void parseIndex(ByteBuffer index) throws IOException {
            siLen = index.getInt();

            int fCnt = index.getInt();
            fsizes = new int[fCnt];
            foffs = new long[fCnt];
            fpts = new long[fCnt];
            long foff = siLen;
            for (int i = 0; i < fCnt; i++) {
                int size = index.getInt();
                fsizes[i] = size;
                foffs[i] = foff;
                foff += size;
            }
            for (int i = 0; i < fCnt; i++) {
                fpts[i] = index.getInt() & 0xffffffffL;
            }
            
            seekToFrame = 0;
            seekToFrame();
            si = pesBuf.duplicate();
            si.limit(si.position());
            si.position(si.limit() - siLen);
        }

        @Override
        public Packet nextFrame() throws IOException {
            seekToFrame();

            int fs = fsizes[curFrame];
            ByteBuffer result = ByteBuffer.allocate(fs + si.remaining());
            result.put(si.duplicate());

            while (result.hasRemaining()) {
                if (pesBuf.hasRemaining()) {
                    result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));
                } else {
                    ++curPesSubIdx;
                    long posShift = 0;
                    if (curPesSubIdx == pesRLE[curPesIdx]) {
                        curPesIdx++;
                        curPesSubIdx = 0;
                        for (; streamId(pesTokens[curPesIdx]) != streamId; curPesIdx++)
                            posShift += (payLoadSize(pesTokens[curPesIdx]) + leadingSize(pesTokens[curPesIdx]))
                                    * pesRLE[curPesIdx];
                    }
                    ch.position(ch.position() + posShift + leadingSize(pesTokens[curPesIdx]));
                    pesBuf = NIOUtils.fetchFrom(ch, payLoadSize(pesTokens[curPesIdx]));
                }
            }
            result.flip();

            Packet pkt = new Packet(result, fpts[curFrame], 90000, 0, curFrame, true, null);

            curFrame++;

            return pkt;
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return null;
        }

        private int payLoadSize(int token) {
            return token & 0xffff;
        }

        private int streamId(int token) {
            return token >>> 24;
        }

        private int leadingSize(int token) {
            return (token >>> 16) & 0xff;
        }

        @Override
        public boolean gotoFrame(long i) {
            seekToFrame = (int) i;

            return true;
        }

        private void seekToFrame() throws IOException {
            if (seekToFrame == -1)
                return;
            curFrame = seekToFrame;

            long payloadOff = foffs[curFrame];
            long fileOff = 0;

            for (curPesIdx = 0;; curPesIdx++) {
                int payloadSize = payLoadSize(pesTokens[curPesIdx]) * pesRLE[curPesIdx];
                int leadingSize = leadingSize(pesTokens[curPesIdx]) * pesRLE[curPesIdx];
                if (streamId(pesTokens[curPesIdx]) == streamId) {
                    if (payloadOff < payloadSize)
                        break;
                    payloadOff -= payloadSize;
                }
                fileOff += payloadSize + leadingSize;
            }
            curPesSubIdx = payloadOff / payLoadSize(pesTokens[curPesIdx]);
            payloadOff = payloadOff % payLoadSize(pesTokens[curPesIdx]);

            fileOff += curPesSubIdx * (payLoadSize(pesTokens[curPesIdx]) + leadingSize(pesTokens[curPesIdx]));
            fileOff += leadingSize(pesTokens[curPesIdx]);
            ch.position(fileOff);
            pesBuf = NIOUtils.fetchFrom(ch, payLoadSize(pesTokens[curPesIdx]));
            NIOUtils.skip(pesBuf, (int) payloadOff);

            seekToFrame = -1;
        }

        @Override
        public long getCurFrame() {
            return curFrame;
        }

        @Override
        public void seek(double second) {
            throw new UnsupportedOperationException();
        }
    }
}
