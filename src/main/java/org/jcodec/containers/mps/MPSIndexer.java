package org.jcodec.containers.mps;

import static org.jcodec.containers.mps.MPSUtils.mediaStream;
import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
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
public class MPSIndexer extends BaseIndexer {
    private long predFileStart;

    public void index(File source, NIOUtils.FileReaderListener listener) throws IOException {
        new NIOUtils.FileReader() {
            protected void data(ByteBuffer data, long filePos) {
                analyseBuffer(data, filePos);
            }
        }.readFile(source, 0x10000, listener);
    }

    protected void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
        if (!mediaStream(stream))
            return;
        PESPacket pesHeader = readPESHeader(pesBuffer, start);
        int leading = 0;
        if (predFileStart != start) {
            leading += (int) (start - predFileStart);
        }
        predFileStart = start + pesLen;
        savePesMeta(stream, leading, pesLen, pesBuffer.remaining());
        getAnalyser(stream).pkt(pesBuffer, pesHeader);
    }

    public ByteBuffer serialize() {
        ByteBuffer buf = ByteBuffer.allocate(estimateSize());
        serializeTo(buf);
        buf.flip();
        return buf;
    }

    public static void main(String[] args) throws IOException {
        MPSIndexer indexer = new MPSIndexer();
        indexer.index(new File(args[0]), new NIOUtils.FileReaderListener() {
            public void progress(int percentDone) {
                System.out.println(percentDone);
            }
        });
        NIOUtils.writeTo(indexer.serialize(), new File(args[1]));
    }
}