package org.jcodec.containers.mps;

import static org.jcodec.containers.mps.MPSUtils.mediaStream;
import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.Assert;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG TS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public class MTSIndexer {
    public static final int BUFFER_SIZE = 188 << 9;
    private MTSAnalyser[] indexers;

    public void index(File source, NIOUtils.FileReaderListener listener) throws IOException {
        index(source, listener, MTSUtils.getMediaPids(source));
    }

    public void index(File source, NIOUtils.FileReaderListener listener, int[] targetGuids) throws IOException {
        indexers = new MTSAnalyser[targetGuids.length];
        for (int i = 0; i < targetGuids.length; i++) {
            indexers[i] = new MTSAnalyser(targetGuids[i]);
        }

        new NIOUtils.FileReader() {
            protected void data(ByteBuffer data, long filePos) {
                analyseBuffer(data, filePos);
            }

            protected void analyseBuffer(ByteBuffer buf, long pos) {
                while (buf.hasRemaining()) {
                    ByteBuffer tsBuf = NIOUtils.read(buf, 188);
                    pos += 188;
                    Assert.assertEquals(0x47, tsBuf.get() & 0xff);
                    int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                    int guid = guidFlags & 0x1fff;

                    for (int i = 0; i < indexers.length; i++) {

                        if (guid == indexers[i].targetGuid) {
                            int payloadStart = (guidFlags >> 14) & 0x1;
                            int b0 = tsBuf.get() & 0xff;
                            int counter = b0 & 0xf;
                            if ((b0 & 0x20) != 0) {
                                NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                            }
                            indexers[i].analyseBuffer(tsBuf, pos - tsBuf.remaining());
                        }
                    }
                }
            }
        }.readFile(source, BUFFER_SIZE, listener);
    }

    public void serializeTo(ByteBuffer buf) {
        for (MTSAnalyser mtsAnalyser : indexers) {
            ByteBuffer dup = buf.duplicate();
            NIOUtils.skip(buf, 4);
            mtsAnalyser.serializeTo(buf);
            dup.putInt(buf.position() - dup.position());
        }
    }

    public ByteBuffer serialize() {
        ByteBuffer buf = ByteBuffer.allocate(estimateSize());
        serializeTo(buf);
        buf.flip();
        return buf;
    }

    public int estimateSize() {
        int totalSize = 0;
        for (MTSAnalyser mtsAnalyser : indexers) {
            totalSize += mtsAnalyser.estimateSize();
        }
        return totalSize;
    }

    private class MTSAnalyser extends BaseIndexer {

        private int targetGuid;
        private long predFileStartInTsPkt;

        public MTSAnalyser(int targetGuid) {
            this.targetGuid = targetGuid;
        }

        @Override
        public void serializeTo(ByteBuffer index) {
            index.putShort((short) targetGuid);
            super.serializeTo(index);
        }

        protected void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
            if (!mediaStream(stream))
                return;
            Logger.debug(String.format("PES: %08x, %d", start, pesLen));
            PESPacket pesHeader = readPESHeader(pesBuffer, start);
            int leadingTsPkt = 0;// pesBuffer.position();
            if (predFileStartInTsPkt != start) {
                leadingTsPkt = (int) (start / 188 - predFileStartInTsPkt);
            }
            predFileStartInTsPkt = (start + pesLen) / 188;
            int tsPktInPes = (int) (predFileStartInTsPkt - start / 188);
            savePesMeta(stream, leadingTsPkt, tsPktInPes, pesBuffer.remaining());
            getAnalyser(stream).pkt(pesBuffer, pesHeader);
        }
    }

    public static void main(String[] args) throws IOException {
        File src = new File(args[0]);

        MTSIndexer indexer = new MTSIndexer();
        indexer.index(src, new NIOUtils.FileReaderListener() {
            public void progress(int percentDone) {
                System.out.println(percentDone);
            }
        });
        NIOUtils.writeTo(indexer.serialize(), new File(args[1]));
    }
}