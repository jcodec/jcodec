package org.jcodec.containers.mps.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mps.MPSUtils;
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

    public MPSRandomAccessDemuxer(SeekableByteChannel ch, MPSIndex mpsIndex) throws IOException {
        pesTokens = mpsIndex.getPesTokens();
        pesStreamIds = mpsIndex.getPesStreamIds().flattern();
        MPSStreamIndex[] streamIndices = mpsIndex.getStreams();
        streams = new Stream[streamIndices.length];
        for (int i = 0; i < streamIndices.length; i++) {
            streams[i] = newStream(ch, streamIndices[i]);
        }
    }

    protected Stream newStream(SeekableByteChannel ch, MPSStreamIndex streamIndex) throws IOException {
        return new Stream(streamIndex, ch);
    }

    public Stream[] getStreams() {
        return streams;
    }

    public class Stream extends MPSStreamIndex implements SeekableDemuxerTrack {

        private static final int MPEG_TIMESCALE = 90000;
        private int curPesIdx;
        private int curFrame;
        private ByteBuffer pesBuf;
        private int seekToFrame = -1;
        protected SeekableByteChannel source;
        private long[] foffs;

        public Stream(MPSStreamIndex streamIndex, SeekableByteChannel source) throws IOException {
            super(streamIndex);
            this.source = source;

            foffs = new long[fsizes.length];
            long curOff = 0;
            for (int i = 0; i < fsizes.length; i++) {
                foffs[i] = curOff;
                curOff += fsizes[i];
            }

            int[] seg = Arrays.copyOf(streamIndex.getFpts(), 100);
            Arrays.sort(seg);

            seekToFrame = 0;
            seekToFrame();
        }

        @Override
        public Packet nextFrame() throws IOException {
            seekToFrame();
            
            if(curFrame >= fsizes.length)
                return null;
            
            int fs = fsizes[curFrame];
            ByteBuffer result = ByteBuffer.allocate(fs);
            return nextFrame(result);
        }

        public Packet nextFrame(ByteBuffer buf) throws IOException {
            seekToFrame();

            if (curFrame >= fsizes.length)
                return null;

            int fs = fsizes[curFrame];

            ByteBuffer result = buf.duplicate();
            result.limit(result.position() + fs);

            while (result.hasRemaining()) {
                if (pesBuf.hasRemaining()) {
                    result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));
                } else {
                    ++curPesIdx;
                    long posShift = 0;
                    while (pesStreamIds[curPesIdx] != streamId) {
                        posShift += MPSIndex.pesLen(pesTokens[curPesIdx]) + MPSIndex.leadingSize(pesTokens[curPesIdx]);
                        ++curPesIdx;
                    }
                    skip(posShift + MPSIndex.leadingSize(pesTokens[curPesIdx]));
                    int pesLen = MPSIndex.pesLen(pesTokens[curPesIdx]);
                    pesBuf = fetch(pesLen);
                    MPSUtils.readPESHeader(pesBuf, 0);
                }
            }
            result.flip();

            Packet pkt = new Packet(result, fpts[curFrame], MPEG_TIMESCALE, fdur[curFrame], curFrame, sync.length == 0
                    || Arrays.binarySearch(sync, curFrame) >= 0, null);

            curFrame++;

            return pkt;
        }

        protected ByteBuffer fetch(int pesLen) throws IOException {
            return NIOUtils.fetchFrom(source, pesLen);
        }
        
        protected void skip(long leadingSize) throws IOException {
            source.position(source.position() + leadingSize);
        }
        
        protected void reset() throws IOException {
            source.position(0);
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
            long posShift = 0;
            
            reset();

            for (curPesIdx = 0;; curPesIdx++) {
                if (pesStreamIds[curPesIdx] == streamId) {
                    int payloadSize = MPSIndex.payLoadSize(pesTokens[curPesIdx]);
                    if (payloadOff < payloadSize)
                        break;
                    payloadOff -= payloadSize;
                }
                posShift += MPSIndex.pesLen(pesTokens[curPesIdx]) + MPSIndex.leadingSize(pesTokens[curPesIdx]);
            }

            skip(posShift + MPSIndex.leadingSize(pesTokens[curPesIdx]));
            pesBuf = fetch(MPSIndex.pesLen(pesTokens[curPesIdx]));
            MPSUtils.readPESHeader(pesBuf, 0);
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
