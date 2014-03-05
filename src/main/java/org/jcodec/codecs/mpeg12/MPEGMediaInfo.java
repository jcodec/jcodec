package org.jcodec.codecs.mpeg12;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Mines for various media info in mpeg file
 * 
 * @author The JCodec project
 * 
 */
public class MPEGMediaInfo {

    private ByteBuffer pesBuffer;
    private int marker;
    private int remaining;
    private boolean lastVideo;

    public MPEGMediaInfo() {
        pesBuffer = ByteBuffer.allocate(0x100000);
        marker = 0xffffffff;
        remaining = Integer.MAX_VALUE;
        lastVideo = true;
    }

    public void getMediaInfo(SeekableByteChannel ch) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(0x10000);
        while (NIOUtils.read(ch, bb) != -1) {
            bb.flip();
            processBuffer(bb);
            bb.clear();
        }
    }

    private void processBuffer(ByteBuffer bb) {
        while (bb.hasRemaining()) {
            pesBuffer.put((byte) (marker >>> 24));
            marker = (marker << 8) | (bb.get() & 0xff);
            remaining--;
            
            if(!lastVideo && pesBuffer.position() == 6) {
                remaining = pesBuffer.getShort(4) & 0xffff;
            }

            if (lastVideo && marker >= 0x1b9 && marker <= 0x1ff || !lastVideo && remaining == 0) {
                System.out.println(String.format("0x%2x", marker));
                pesBuffer.flip();

                remaining = Integer.MAX_VALUE;
                
                processPes(pesBuffer);
                
                pesBuffer.clear();
                lastVideo = marker >= 0x1e0 && marker <= 0x1ef || marker == 0x1ba;
            }
        }
    }

    private void processPes(ByteBuffer buf) {
        int marker = buf.getInt();
        if(marker == 0x1bc) {
            parsePSM(buf);
        }
    }

    private void parsePSM(ByteBuffer pesBuffer) {
        pesBuffer.getInt();
        short psmLen = pesBuffer.getShort();
        if (psmLen > 1018)
            throw new RuntimeException("Invalid PSM");
        byte b0 = pesBuffer.get();
        byte b1 = pesBuffer.get();
        if ((b1 & 1) != 1)
            throw new RuntimeException("Invalid PSM");
        short psiLen = pesBuffer.getShort();
        ByteBuffer psi = NIOUtils.read(pesBuffer, psiLen & 0xffff);
        short elStreamLen = pesBuffer.getShort();
        parseElStreams(NIOUtils.read(pesBuffer, elStreamLen & 0xffff));
        int crc = pesBuffer.getInt();
    }

    private void parseElStreams(ByteBuffer buf) {
        while (buf.hasRemaining()) {
            byte streamType = buf.get();
            byte streamId = buf.get();
            short strInfoLen = buf.getShort();
            ByteBuffer strInfo = NIOUtils.read(buf, strInfoLen & 0xffff);
        }
    }

    public static void main(String[] args) throws IOException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableFileChannel(new File(args[0]));
            new MPEGMediaInfo().getMediaInfo(ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Extract stream types from a program stream map According to ISO/IEC
     * 13818-1 ('MPEG-2 Systems') table 2-35
     * 
     * @return number of bytes occupied by PSM in the bitstream
     */
    // static long mpegps_psm_parse(MpegDemuxContext *m, AVIOContext *pb)
    // {
    // int psm_length, ps_info_length, es_map_length;
    //
    // psm_length = avio_rb16(pb);
    // avio_r8(pb);
    // avio_r8(pb);
    // ps_info_length = avio_rb16(pb);
    //
    // /* skip program_stream_info */
    // avio_skip(pb, ps_info_length);
    // es_map_length = avio_rb16(pb);
    //
    // /* at least one es available? */
    // while (es_map_length >= 4){
    // unsigned char type = avio_r8(pb);
    // unsigned char es_id = avio_r8(pb);
    // uint16_t es_info_length = avio_rb16(pb);
    // /* remember mapping from stream id to stream type */
    // m->psm_es_type[es_id] = type;
    // /* skip program_stream_info */
    // avio_skip(pb, es_info_length);
    // es_map_length -= 4 + es_info_length;
    // }
    // avio_rb32(pb); /* crc32 */
    // return 2 + psm_length;
    // }
}
