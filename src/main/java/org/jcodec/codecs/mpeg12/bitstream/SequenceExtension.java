package org.jcodec.codecs.mpeg12.bitstream;

import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceExtension implements MPEGHeader {

    public static final int Chroma420 = 0x1;
    public static final int Chroma422 = 0x2;
    public static final int Chroma444 = 0x3;

    public int profile_and_level;
    public int progressive_sequence;
    public int chroma_format;
    public int horizontal_size_extension;
    public int vertical_size_extension;
    public int bit_rate_extension;
    public int vbv_buffer_size_extension;
    public int low_delay;
    public int frame_rate_extension_n;
    public int frame_rate_extension_d;

    public SequenceExtension(int profile_and_level, int progressive_sequence, int chroma_format,
            int horizontal_size_extension, int vertical_size_extension, int bit_rate_extension,
            int vbv_buffer_size_extension, int low_delay, int frame_rate_extension_n, int frame_rate_extension_d) {
        this.profile_and_level = profile_and_level;
        this.progressive_sequence = progressive_sequence;
        this.chroma_format = chroma_format;
        this.horizontal_size_extension = horizontal_size_extension;
        this.vertical_size_extension = vertical_size_extension;
        this.bit_rate_extension = bit_rate_extension;
        this.vbv_buffer_size_extension = vbv_buffer_size_extension;
        this.low_delay = low_delay;
        this.frame_rate_extension_n = frame_rate_extension_n;
        this.frame_rate_extension_d = frame_rate_extension_d;
    }

    private SequenceExtension() {
    }

    public static SequenceExtension read(BitReader in) {
        SequenceExtension se = new SequenceExtension();
        se.profile_and_level = in.readNBit(8);
        se.progressive_sequence = in.read1Bit();
        se.chroma_format = in.readNBit(2);
        se.horizontal_size_extension = in.readNBit(2);
        se.vertical_size_extension = in.readNBit(2);
        se.bit_rate_extension = in.readNBit(12);
        se.vbv_buffer_size_extension = in.readNBit(8);
        se.low_delay = in.read1Bit();
        se.frame_rate_extension_n = in.readNBit(2);
        se.frame_rate_extension_d = in.readNBit(5);

        return se;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(SequenceHeader.Sequence_Extension, 4);
        bw.writeNBit(profile_and_level, 8);
        bw.write1Bit(progressive_sequence);
        bw.writeNBit(chroma_format, 2);
        bw.writeNBit(horizontal_size_extension, 2);
        bw.writeNBit(vertical_size_extension, 2);
        bw.writeNBit(bit_rate_extension, 12);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(vbv_buffer_size_extension, 8);
        bw.write1Bit(low_delay);
        bw.writeNBit(frame_rate_extension_n, 2);
        bw.writeNBit(frame_rate_extension_d, 5);
        
        bw.flush();
    }
}