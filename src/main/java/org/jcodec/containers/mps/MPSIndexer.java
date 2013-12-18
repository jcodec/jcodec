package org.jcodec.containers.mps;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG PS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public class MPSIndexer {

    private int marker = -1;
    private int lenFieldLeft;
    private int pesLen;
    private long pesFileStart = -1;
    private int stream;
    private boolean pes;
    private int pesLeft;
    private Map<Integer, BaseAnalyser> analyzers = new HashMap<Integer, BaseAnalyser>();
    private ByteBuffer pesBuffer;
    private int numPesEntries;
    private long predFileStart;
    private ByteBuffer index;
    private int prevPesToken;
    private int runLengthMinus1 = -1;

    public ByteBuffer index(File source, ByteBuffer indexBuf) throws IOException {
        index = indexBuf.duplicate();
        SeekableByteChannel ch = null;
        ByteBuffer buf = ByteBuffer.allocate(0x10000);
        pesBuffer = ByteBuffer.allocate(0x10000);
        NIOUtils.skip(index, 4);

        try {
            ch = NIOUtils.readableFileChannel(source);
            for (long pos = ch.position(); ch.read(buf) != -1; pos = ch.position()) {
                buf.flip();
                analyseBuffer(buf, pos);
                buf.flip();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
        putRLToken();

        for (Integer stream : analyzers.keySet()) {
            index.put((byte) stream.intValue());
            analyzers.get(stream).serialize(index);
        }
        index.flip();
        index.duplicate().putInt(numPesEntries + 1);
        return index;
    }

    private abstract class BaseAnalyser {
        private IntArrayList pts = new IntArrayList(250000);

        public abstract void pkt(ByteBuffer pkt, PESPacket pesHeader);

        public abstract void serialize(ByteBuffer bb);

        public void framePts(PESPacket pesHeader) {
            pts.add((int)pesHeader.pts);
        }

        public void serializePts(ByteBuffer bb) {
            for(int i = 0; i < pts.size(); i++)
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

            super.serializePts(bb);
        }
    }

    private class MPEGVideoAnalyser extends BaseAnalyser {
        private int marker = -1;
        private long position;
        private long prevFrame = -1;
        private IntArrayList sizes = new IntArrayList(250000);
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
                        super.framePts(pesHeader);
                    } else
                        siSize = (int) frameStart;

                    prevFrame = frameStart;
                    frameNo ++;
                }
            }
        }

        public void serialize(ByteBuffer bb) {
            bb.putInt(siSize);
            int[] array = sizes.toArray();
            bb.putInt(array.length);
            for (int i = 0; i < array.length; i++)
                bb.putInt(array[i]);
            
            super.serializePts(bb);
        }
    }

    private void analyseBuffer(ByteBuffer buf, long pos) {
        int init = buf.position();
        while (buf.hasRemaining()) {
            if (pesLeft > 0) {
                int toRead = Math.min(buf.remaining(), pesLeft);
                pesBuffer.put(NIOUtils.read(buf, toRead));
                pesLeft -= toRead;

                if (pesLeft == 0) {
                    pes(pesFileStart, stream);
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
            if (marker >= 0x1bd && marker <= 0x1ef) {
                long filePos = pos + buf.position() - init - 4;
                if (pes)
                    pes(pesFileStart, stream);
                pesFileStart = filePos;

                pes = true;
                stream = marker & 0xff;
                lenFieldLeft = 2;
                pesLen = 0;
            } else if (marker >= 0x1b9 && marker <= 0x1ff) {
                if (pes)
                    pes(pesFileStart, stream);
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

    private BaseAnalyser getAnalyser(int stream) {
        BaseAnalyser analizer = analyzers.get(stream);
        if (analizer == null) {
            analizer = stream >= 0xe0 && stream <= 0xef ? new MPEGVideoAnalyser() : new GenericAnalyser();
            analyzers.put(stream, analizer);
        }
        return analyzers.get(stream);
    }

    private void pes(long start, int stream) {
        pesBuffer.flip();
        int pesLen = pesBuffer.remaining();
        PESPacket pesHeader = MPSDemuxer.readPES(pesBuffer, start);
        int leading = pesBuffer.position();
        if (predFileStart != start) {
            leading += (int) (start - predFileStart);
        }
        System.out.println(leading);
        predFileStart = start + pesLen;
        writePesToken(stream, leading);
        getAnalyser(stream).pkt(pesBuffer, pesHeader);
        pesBuffer.clear();
    }

    private void writePesToken(int stream, int leading) {
        int pesToken = (stream << 24) | (leading << 16) | pesBuffer.remaining();
        if (prevPesToken != 0 && pesToken != prevPesToken || runLengthMinus1 >= 0x7fff) {
            putRLToken();
            runLengthMinus1 = 0;
            ++numPesEntries;
        } else
            runLengthMinus1++;
        prevPesToken = pesToken;
    }

    private void putRLToken() {
        if (runLengthMinus1 <= 0x7f)
            index.put((byte) runLengthMinus1);
        else {
            index.put((byte) ((runLengthMinus1 >> 8) | 0x80));
            index.put((byte) (runLengthMinus1 & 0xff));
        }

        index.putInt(prevPesToken);
    }
}