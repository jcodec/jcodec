package org.jcodec.containers.mps.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mps.index.MPSIndex.MPSStreamIndex;

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

    private Stream[] streams;
    private long[] pesTokens;
    private int[] pesStreamIds;

    public MPSRandomAccessDemuxer(SeekableByteChannel ch, ByteBuffer index) throws IOException {
        MPSIndex mpsIndex = MPSIndex.parseIndex(index);
        pesTokens = mpsIndex.getPesTokens();
        pesStreamIds = mpsIndex.getPesStreamIds().flattern();
        MPSStreamIndex[] streamIndices = mpsIndex.getStreams();
        for (int i = 0; i < streamIndices.length; i++) {
            streams[i] = new Stream(streamIndices[i], ch);
        }
    }

    public Stream[] getStreams() {
        return streams;
    }

    public class Stream extends MPSStreamIndex implements SeekableDemuxerTrack {

        private static final int MPEG_TIMESCALE = 90000;
        private int curPesIdx;
        private int curFrame;
        private ByteBuffer si;
        private ByteBuffer pesBuf;
        private MPSStreamIndex streamIndex;
        private int seekToFrame = -1;
        private SeekableByteChannel source;
        private long duration;
        private long avgFrameduration;
        private long[] foffs;

        public Stream(MPSStreamIndex streamIndex, SeekableByteChannel source) throws IOException {
            super(streamIndex);
            this.streamIndex = streamIndex;
            this.source = source;

            foffs = new long[fsizes.length];
            long curOff = siLen;
            for (int i = 0; i < fsizes.length; i++) {
                foffs[i] = curOff;
                curOff += fsizes[i];
            }

            int[] seg = Arrays.copyOf(streamIndex.getFpts(), 100);
            Arrays.sort(seg);

            avgFrameduration = (seg[99] - seg[0]) / 100;

            seekToFrame = 0;
            seekToFrame();
            ByteBuffer si = pesBuf.duplicate();
            si.limit(si.position());
            si.position(si.limit() - siLen);
        }

        @Override
        public Packet nextFrame() throws IOException {
            int fs = fsizes[curFrame];
            ByteBuffer result = ByteBuffer.allocate(fs + si.remaining());
            return nextFrame(result);
        }

        public Packet nextFrame(ByteBuffer buf) throws IOException {
            seekToFrame();

            if (curFrame >= fsizes.length)
                return null;

            int fs = fsizes[curFrame];

            ByteBuffer result = buf.duplicate();
            result.limit(result.position() + fs + si.remaining());

            result.put(si.duplicate());

            while (result.hasRemaining()) {
                if (pesBuf.hasRemaining()) {
                    result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));
                } else {
                    ++curPesIdx;
                    long posShift = 0;
                    while (pesStreamIds[curPesIdx] != streamId) {
                        posShift += MPSIndex.pesLen(pesTokens[curPesIdx]);
                        ++curPesIdx;
                    }
                    source.position(source.position() + posShift + MPSIndex.leadingSize(pesTokens[curPesIdx]));
                    pesBuf = NIOUtils.fetchFrom(source, MPSIndex.payLoadSize(pesTokens[curPesIdx]));
                }
            }
            result.flip();

            Packet pkt = new Packet(result, fpts[curFrame], MPEG_TIMESCALE, duration, curFrame, sync.length == 0
                    || Arrays.binarySearch(sync, curFrame) >= 0, null);

            curFrame++;

            return pkt;
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return null;
        }

        @Override
        public boolean gotoFrame(long frameNo) {
            seekToFrame = (int) frameNo;

            return true;
        }

        @Override
        public boolean gotoSyncFrame(long frameNo) {
            for (int i = 0; i < sync.length; i++) {
                if (sync[i] > frameNo) {
                    seekToFrame = sync[i - 1];
                    return true;
                }
            }
            seekToFrame = sync[sync.length - 1];
            return true;
        }

        private void seekToFrame() throws IOException {
            if (seekToFrame == -1)
                return;
            curFrame = seekToFrame;

            long payloadOff = foffs[curFrame];
            long fileOff = 0;

            for (curPesIdx = 0;; curPesIdx++) {
                if (pesStreamIds[curPesIdx] == streamId) {
                    int payloadSize = MPSIndex.payLoadSize(pesTokens[curPesIdx]);
                    if (payloadOff < payloadSize)
                        break;
                    payloadOff -= payloadSize;
                }
                fileOff += MPSIndex.pesLen(pesTokens[curPesIdx]);
            }

            fileOff += MPSIndex.leadingSize(pesTokens[curPesIdx]);
            source.position(fileOff);
            pesBuf = NIOUtils.fetchFrom(source, MPSIndex.payLoadSize(pesTokens[curPesIdx]));
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
