package org.jcodec.codecs.mpeg12.bitstream;

import java.io.IOException;

import org.jcodec.common.io.BitstreamReaderBB;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceExtension {
    
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

    public static SequenceExtension read(BitstreamReaderBB in) {
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

    public void write(OutBits out) throws IOException {
        out.writeNBit(profile_and_level, 8);
        out.write1Bit(progressive_sequence);
        out.writeNBit(chroma_format, 2);
        out.writeNBit(horizontal_size_extension, 2);
        out.writeNBit(vertical_size_extension, 2);
        out.writeNBit(bit_rate_extension, 12);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(vbv_buffer_size_extension, 8);
        out.write1Bit(low_delay);
        out.writeNBit(frame_rate_extension_n, 2);
        out.writeNBit(frame_rate_extension_d, 5);
    }
}