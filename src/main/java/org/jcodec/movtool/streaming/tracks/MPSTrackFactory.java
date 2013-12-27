package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A factory for MPEG PS virtual tracks coming out of streams of MPEG PS
 * 
 * @author The JCodec project
 * 
 */
public class MPSTrackFactory {

    private int[] pesTokens;
    private short[] pesRLE;
    private Map<Integer, Stream> streams;
    private FilePool fp;

    public MPSTrackFactory(ByteBuffer index, FilePool fp) throws IOException {
        this.fp = fp;
        streams = new HashMap<Integer, Stream>();

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
    }

    private Stream getStream(Map<Integer, Stream> streams, int streamId) {
        Stream stream = streams.get(streamId);
        if (stream == null) {
            stream = new Stream(streamId);
            streams.put(streamId, stream);
        }
        return stream;
    }

    public class Stream implements VirtualTrack {

        private int siLen;
        private int[] fsizes;
        private long[] fpts;
        private int[] sync;
        private long duration;
        private int streamId;
        private long fileOff;
        private int curPesIdx;
        private int curFrame;
        private int curPesSubIdx;
        private int pesOff;
        private ByteBuffer si;

        public Stream(int streamId) {
            this.streamId = streamId;
        }

        public void parseIndex(ByteBuffer index) throws IOException {
            siLen = index.getInt();

            int fCnt = index.getInt();
            fsizes = new int[fCnt];
            fpts = new long[fCnt];
            for (int i = 0; i < fCnt; i++) {
                int size = index.getInt();
                fsizes[i] = size;
            }

            int syncCount = index.getInt();
            sync = new int[syncCount];
            for (int i = 0; i < syncCount; i++)
                sync[i] = index.getInt();

            for (int i = 0; i < fCnt; i++) {
                fpts[i] = index.getInt() & 0xffffffffL;
            }

            long[] seg0 = Arrays.copyOf(fpts, 10);
            Arrays.sort(seg0);
            
            long[] seg1 = new long[10];
            System.arraycopy(fpts, fpts.length - 10, seg1, 0, 10);
            Arrays.sort(seg1);

            duration = (seg1[9] - seg0[0] + (fpts.length >> 1)) / fpts.length;

            pesOff = siLen;
            for (fileOff = 0; streamId(pesTokens[curPesIdx]) != streamId; fileOff += (payLoadSize(pesTokens[curPesIdx]) + leadingSize(pesTokens[curPesIdx]))
                    * pesRLE[curPesIdx], curPesIdx++)
                ;
            fileOff += leadingSize(pesTokens[curPesIdx]);
            SeekableByteChannel ch = null;
            try {
                ch = fp.getChannel();
                ch.position(fileOff);
                si = NIOUtils.fetchFrom(ch, siLen);
            } finally {
                NIOUtils.closeQuietly(ch);
            }
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
        public VirtualPacket nextPacket() throws IOException {
            if(curFrame >= fsizes.length)
                return null;
            VirtualPacket pkt = new MPSPacket(curPesIdx, curPesSubIdx, pesOff, fileOff, curFrame);

            pesOff += fsizes[curFrame];
            
            while (pesOff >= payLoadSize(pesTokens[curPesIdx])) {
                int ps = payLoadSize(pesTokens[curPesIdx]);
                pesOff -= ps;
                fileOff += ps;
                ++curPesSubIdx;
                long posShift = 0;
                if (curPesSubIdx == pesRLE[curPesIdx] && curPesIdx < pesTokens.length - 1) {
                    curPesIdx++;
                    curPesSubIdx = 0;
                    for (; streamId(pesTokens[curPesIdx]) != streamId && curPesIdx < pesTokens.length - 1; curPesIdx++)
                        posShift += (payLoadSize(pesTokens[curPesIdx]) + leadingSize(pesTokens[curPesIdx]))
                                * pesRLE[curPesIdx];
                }
                fileOff += posShift + leadingSize(pesTokens[curPesIdx]);
            }
            curFrame++;

            return pkt;
        }

        private class MPSPacket implements VirtualPacket {

            private int curPesIdx;
            private int curPesSubIdx;
            private long fileOff;
            private int curFrame;
            private int pesOff;

            public MPSPacket(int curPesIdx, int curPesSubIdx, int pesOff, long fileOff, int curFrame) {
                this.curPesIdx = curPesIdx;
                this.curPesSubIdx = curPesSubIdx;
                this.pesOff = pesOff;
                this.fileOff = fileOff;
                this.curFrame = curFrame;
            }

            @Override
            public ByteBuffer getData() throws IOException {
                ByteBuffer result = ByteBuffer.allocate(siLen + fsizes[curFrame]);
                result.put(si.duplicate());
                SeekableByteChannel ch = null;
                try {
                    ch = fp.getChannel();
                    ch.position(fileOff);
                    ByteBuffer pesBuf = NIOUtils.fetchFrom(ch, payLoadSize(pesTokens[curPesIdx]));
                    NIOUtils.skip(pesBuf, pesOff);
//                    System.out.println(String.format("Frame %d: %012x", curFrame, fileOff + pesOff));
                    result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));

                    for (int idx = curPesIdx, sidx = curPesSubIdx; result.hasRemaining();) {
                        ++sidx;
                        long posShift = 0;
                        if (sidx == pesRLE[idx] && idx < pesTokens.length - 1) {
                            idx++;
                            sidx = 0;
                            for (; streamId(pesTokens[idx]) != streamId && idx < pesTokens.length; idx++)
                                posShift += (payLoadSize(pesTokens[idx]) + leadingSize(pesTokens[idx])) * pesRLE[idx];
                        }
                        ch.position(ch.position() + posShift + leadingSize(pesTokens[idx]));
                        pesBuf = NIOUtils.fetchFrom(ch, payLoadSize(pesTokens[idx]));
                        result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));
                    }
                    result.flip();

                    return result;
                } finally {
                    NIOUtils.closeQuietly(ch);
                }
            }

            @Override
            public int getDataLen() throws IOException {
                return siLen + fsizes[curFrame];
            }

            @Override
            public double getPts() {
                return (double) (fpts[curFrame] - fpts[0]) / 90000;
            }

            @Override
            public double getDuration() {
                return (double) duration / 90000;
            }

            @Override
            public boolean isKeyframe() {
                return sync.length == 0 || Arrays.binarySearch(sync, curFrame) >= 0;
            }

            @Override
            public int getFrameNo() {
                return curFrame;
            }
        }

        @Override
        public SampleEntry getSampleEntry() {
            return new VideoSampleEntry(new Header("m2v1"), (short) 0, (short) 0, "jcod", 0, 768, (short) 1920,
                    (short) 1080, 72, 72, (short) 1, "jcodec", (short) 25, (short) 1, (short) -1);
        }

        @Override
        public VirtualEdit[] getEdits() {
            return null;
        }

        @Override
        public int getPreferredTimescale() {
            return 90000;
        }

        @Override
        public void close() throws IOException {
            fp.close();
        }
    }

    public List<Stream> getStreams() {
        return new ArrayList<Stream>(streams.values());
    }
}
