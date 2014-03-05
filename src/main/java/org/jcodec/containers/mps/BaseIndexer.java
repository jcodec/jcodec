package org.jcodec.containers.mps;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.RunLength;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG PS/TS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public abstract class BaseIndexer {

    private int marker = -1;
    private int lenFieldLeft;
    private int pesLen;
    private long pesFileStart = -1;
    private int stream;
    private boolean pes;
    private int pesLeft;

    private ByteBuffer pesBuffer = ByteBuffer.allocate(1 << 21);

    private Map<Integer, BaseAnalyser> analyzers = new HashMap<Integer, BaseAnalyser>();
    private LongArrayList tokens = new LongArrayList();
    private RunLength.Integer streams = new RunLength.Integer();

    protected abstract void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream);

    protected void analyseBuffer(ByteBuffer buf, long pos) {
        int init = buf.position();
        while (buf.hasRemaining()) {
            if (pesLeft > 0) {
                int toRead = Math.min(buf.remaining(), pesLeft);
                pesBuffer.put(NIOUtils.read(buf, toRead));
                pesLeft -= toRead;

                if (pesLeft == 0) {
                    long filePos = pos + buf.position() - init;
                    pes(pesBuffer, pesFileStart, (int) (filePos - pesFileStart), stream);
                    pesFileStart = -1;
                    pes = false;
                    stream = -1;
                }
                continue;
            }
            int bt = buf.get() & 0xff;
            if (pes)
                pesBuffer.put((byte) (marker >>> 24));
            marker = (marker << 8) | bt;
            if (marker == 0x1bd || marker == 0x1bf || marker >= 0x1c0 && marker <= 0x1ef) {
                long filePos = pos + buf.position() - init - 4;
                if (pes)
                    pes(pesBuffer, pesFileStart, (int) (filePos - pesFileStart), stream);
                pesFileStart = filePos;

                pes = true;
                stream = marker & 0xff;
                lenFieldLeft = 2;
                pesLen = 0;
            } else if (marker >= 0x1b9 && marker <= 0x1ff) {
                if (pes) {
                    long filePos = pos + buf.position() - init - 4;
                    pes(pesBuffer, pesFileStart, (int) (filePos - pesFileStart), stream);
                }
                pesFileStart = -1;
                pes = false;
                stream = -1;
            } else if (lenFieldLeft > 0) {
                pesLen = (pesLen << 8) | bt;
                lenFieldLeft--;
                if (lenFieldLeft == 0) {
                    pesLeft = pesLen;
                    if (pesLen != 0) {
                        pesBuffer.put((byte) (marker >>> 24));
                        pesBuffer.put((byte) ((marker >>> 16) & 0xff));
                        pesBuffer.put((byte) ((marker >>> 8) & 0xff));
                        pesBuffer.put((byte) (marker & 0xff));
                        marker = -1;
                    }
                }
            }
        }
    }

    public int estimateSize() {
        int sizeEstimate = (tokens.size() << 3) + streams.estimateSize() + 128;
        for (Integer stream : analyzers.keySet()) {
            sizeEstimate += analyzers.get(stream).estimateSize();
        }
        return sizeEstimate;
    }

    public void serializeTo(ByteBuffer index) {
        index.putInt(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            index.putLong(tokens.get(i));
        }
        streams.serialize(index);

        for (Integer stream : analyzers.keySet()) {
            index.put((byte) stream.intValue());
            analyzers.get(stream).serialize(index);
        }
    }

    protected abstract class BaseAnalyser {
        private IntArrayList pts = new IntArrayList(250000);

        public abstract void pkt(ByteBuffer pkt, PESPacket pesHeader);

        public int estimateSize() {
            return (pts.size() << 2) + 4;
        }

        public abstract void serialize(ByteBuffer bb);

        public void framePts(PESPacket pesHeader) {
            if (pesHeader.pts == -1)
                pts.add(pts.get(pts.size() - 1));
            else
                pts.add((int) pesHeader.pts);
        }

        public void serializePts(ByteBuffer bb) {
            for (int i = 0; i < pts.size(); i++)
                bb.putInt(pts.get(i));
        }
    }

    private class GenericAnalyser extends BaseAnalyser {
        private IntArrayList sizes = new IntArrayList(250000);

        public void pkt(ByteBuffer pkt, PESPacket pesHeader) {
            sizes.add(pkt.remaining());
            super.framePts(pesHeader);
        }

        public void serialize(ByteBuffer bb) {
            bb.putInt(0);
            int[] array = sizes.toArray();
            bb.putInt(array.length);
            for (int i = 0; i < array.length; i++)
                bb.putInt(array[i]);
            bb.putInt(0); // key frames table

            super.serializePts(bb);
        }

        @Override
        public int estimateSize() {
            return super.estimateSize() + (sizes.size() << 2) + 32;
        }
    }

    private class MPEGVideoAnalyser extends BaseAnalyser {
        private int marker = -1;
        private long position;
        private long prevFrame = -1;
        private IntArrayList sizes = new IntArrayList(250000);
        private IntArrayList keyFrames = new IntArrayList(20000);
        private int siSize;
        private int frameNo;

        public void pkt(ByteBuffer pkt, PESPacket pesHeader) {

            while (pkt.hasRemaining()) {
                int b = pkt.get() & 0xff;
                ++position;
                marker = (marker << 8) | b;
                if (marker == 0x100) {
                    long frameStart = position - 4;
                    if (prevFrame != -1) {
                        sizes.add((int) (frameStart - prevFrame));
                    } else
                        siSize = (int) frameStart;

                    super.framePts(pesHeader);
                    prevFrame = frameStart;
                    System.out.println(String.format("FRAME[%d]: %012x, %d", frameNo,
                            (pesHeader.pos + pkt.position() - 4), pesHeader.pts));
                    frameNo++;
                }
                if (position - prevFrame == 6) {
                    int picCodingType = (b >> 3) & 0x7;
                    if (picCodingType == PictureHeader.IntraCoded)
                        keyFrames.add(frameNo - 1);
                }
            }
        }

        public void serialize(ByteBuffer bb) {
            bb.putInt(siSize);
            int[] array = sizes.toArray();
            bb.putInt(array.length + 1);
            for (int i = 0; i < array.length; i++)
                bb.putInt(array[i]);
            bb.putInt((int) (position - prevFrame));
            bb.putInt(keyFrames.size());
            for (int i = 0; i < keyFrames.size(); i++)
                bb.putInt(keyFrames.get(i));

            super.serializePts(bb);
        }

        @Override
        public int estimateSize() {
            return super.estimateSize() + ((sizes.size() + keyFrames.size()) << 2) + 64;
        }
    }

    protected BaseAnalyser getAnalyser(int stream) {
        BaseAnalyser analizer = analyzers.get(stream);
        if (analizer == null) {
            analizer = stream >= 0xe0 && stream <= 0xef ? new MPEGVideoAnalyser() : new GenericAnalyser();
            analyzers.put(stream, analizer);
        }
        return analyzers.get(stream);
    }

    protected void savePesMeta(int stream, long leading, long pesLen, long payloadLen) {
        long token = (leading << 48) | (pesLen << 24) | payloadLen;
        tokens.add(token);
        streams.add(stream);
    }
}
