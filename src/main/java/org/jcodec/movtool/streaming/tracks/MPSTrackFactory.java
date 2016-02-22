package org.jcodec.movtool.streaming.tracks;

import static java.lang.System.arraycopy;
import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jcodec.common.RunLength;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VideoCodecMeta;
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

    private Map<Integer, Stream> tracks;
    private FilePool fp;
    private long[] pesTokens;
    private int[] streams;

    public MPSTrackFactory(ByteBuffer index, FilePool fp) throws IOException {
        this.fp = fp;
        tracks = new HashMap<Integer, Stream>();

        readIndex(index);
    }

    protected void readIndex(ByteBuffer index) throws IOException {
        int nTokens = index.getInt();
        pesTokens = new long[nTokens];
        for (int i = 0; i < pesTokens.length; i++)
            pesTokens[i] = index.getLong();
        streams = RunLength.Integer.parse(index).flattern();

        while (index.hasRemaining()) {
            int stream = index.get() & 0xff;
            getStream(tracks, stream).parseIndex(index);
        }
    }

    private Stream getStream(Map<Integer, Stream> streams, int streamId) {
        Stream stream = streams.get(streamId);
        if (stream == null) {
            stream = createStream(streamId);
            streams.put(streamId, stream);
        }
        return stream;
    }

    protected Stream createStream(int streamId) {
        return new Stream(streamId);
    }

    public class Stream implements VirtualTrack {

        private int siLen;
        private int[] fsizes;
        private long[] fpts;
        private int[] sync;
        private long duration;
        private int streamId;
        private long fileOff;
        private int pesIdx;
        private int curFrame;
        private int offInPayload;
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
            arraycopy(fpts, fpts.length - 10, seg1, 0, 10);
            Arrays.sort(seg1);

            duration = (seg1[9] - seg0[0] + (fpts.length >> 1)) / fpts.length;

            offInPayload = siLen;
            for (fileOff = 0; streams[pesIdx] != streamId; fileOff += pesLen(pesTokens[pesIdx])
                    + leadingSize(pesTokens[pesIdx]), pesIdx++)
                ;
            fileOff += leadingSize(pesTokens[pesIdx]);

            SeekableByteChannel ch = null;
            try {
                ch = fp.getChannel();
                ByteBuffer firstPes = readPes(ch, fileOff, pesLen(pesTokens[pesIdx]), payloadLen(pesTokens[pesIdx]),
                        pesIdx);
                si = NIOUtils.read(firstPes, siLen);
            } finally {
                NIOUtils.closeQuietly(ch);
            }
        }

        protected ByteBuffer readPes(SeekableByteChannel ch, long pesPosition, int pesSize, int payloadSize, int pesIdx)
                throws IOException {
            ch.position(pesPosition);
            ByteBuffer pes = NIOUtils.fetchFrom(ch, pesSize);
            readPESHeader(pes, 0);
            return pes;
        }

        private int pesLen(long token) {
            return (int) ((token >>> 24) & 0xffffff);
        }

        private int payloadLen(long token) {
            return (int) (token & 0xffffff);
        }

        private int leadingSize(long token) {
            return (int) ((token >>> 48) & 0xffff);
        }

        @Override
        public VirtualPacket nextPacket() throws IOException {
            if (curFrame >= fsizes.length)
                return null;
            VirtualPacket pkt = new MPSPacket(offInPayload, fileOff, curFrame, pesIdx);

            offInPayload += fsizes[curFrame];

            while (pesIdx < streams.length && offInPayload >= payloadLen(pesTokens[pesIdx])) {
                int ps = payloadLen(pesTokens[pesIdx]);
                offInPayload -= ps;
                fileOff += pesLen(pesTokens[pesIdx]);
                ++pesIdx;
                if (pesIdx < streams.length) {
                    long posShift = 0;
                    for (; streams[pesIdx] != streamId; pesIdx++)
                        posShift += pesLen(pesTokens[pesIdx]) + leadingSize(pesTokens[pesIdx]);
                    fileOff += posShift + leadingSize(pesTokens[pesIdx]);
                }
            }
            curFrame++;

            return pkt;
        }

        protected class MPSPacket implements VirtualPacket {

            private long fileOff;
            private int curFrame;
            private int pesOff;
            private int pesIdx;

            public MPSPacket(int pesOff, long fileOff, int curFrame, int pesIdx) {
                this.pesOff = pesOff;
                this.fileOff = fileOff;
                this.curFrame = curFrame;
                this.pesIdx = pesIdx;
            }

            @Override
            public ByteBuffer getData() throws IOException {
                ByteBuffer result = ByteBuffer.allocate(siLen + fsizes[curFrame]);
                result.put(si.duplicate());
                SeekableByteChannel ch = null;
                try {
                    ch = fp.getChannel();

                    long curOff = fileOff;
                    ByteBuffer pesBuf = readPes(ch, curOff, pesLen(pesTokens[pesIdx]), payloadLen(pesTokens[pesIdx]),
                            pesIdx);
                    curOff += pesLen(pesTokens[pesIdx]);

                    NIOUtils.skip(pesBuf, pesOff);
                    result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));

                    for (int idx = pesIdx; result.hasRemaining();) {
                        long posShift = 0;
                        idx++;
                        for (; streams[idx] != streamId && idx < pesTokens.length; idx++)
                            posShift += pesLen(pesTokens[idx]) + leadingSize(pesTokens[idx]);

                        pesBuf = readPes(ch, curOff + posShift + leadingSize(pesTokens[idx]), pesLen(pesTokens[idx]),
                                payloadLen(pesTokens[idx]), idx);
                        curOff += posShift + leadingSize(pesTokens[idx]) + pesLen(pesTokens[idx]);

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
        public CodecMeta getCodecMeta() {
            return new VideoCodecMeta("m2v1", ByteBuffer.allocate(0), new Size(1920, 1080), new Rational(1, 1));
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

    public List<Stream> getVideoStreams() {
        List<Stream> ret = new ArrayList<Stream>();
        Set<Entry<Integer, Stream>> entrySet = tracks.entrySet();
        for (Entry<Integer, Stream> entry : entrySet) {
            if (MPSUtils.videoStream(entry.getKey()))
                ret.add(entry.getValue());
        }
        return ret;
    }

    public List<Stream> getAudioStreams() {
        List<Stream> ret = new ArrayList<Stream>();
        Set<Entry<Integer, Stream>> entrySet = tracks.entrySet();
        for (Entry<Integer, Stream> entry : entrySet) {
            if (MPSUtils.audioStream(entry.getKey()))
                ret.add(entry.getValue());
        }
        return ret;
    }

    public List<Stream> getStreams() {
        return new ArrayList<Stream>(tracks.values());
    }

    public static void main(String[] args) throws IOException {
        FilePool fp = new FilePool(new File(args[0]), 10);
        MPSTrackFactory factory = new MPSTrackFactory(NIOUtils.fetchFrom(new File(args[1])), fp);
        Stream stream = factory.getVideoStreams().get(0);
        FileChannelWrapper ch = NIOUtils.writableFileChannel(new File(args[2]));

        List<VirtualPacket> pkt = new ArrayList<VirtualPacket>();
        for (int i = 0; i < 2000; i++) {
            pkt.add(stream.nextPacket());
        }

        for (VirtualPacket virtualPacket : pkt) {
            ch.write(virtualPacket.getData());
        }

        ch.close();
    }
}
