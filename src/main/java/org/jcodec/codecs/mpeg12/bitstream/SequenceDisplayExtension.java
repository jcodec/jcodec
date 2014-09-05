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
public class SequenceDisplayExtension implements MPEGHeader {
    public int video_format;
    public int display_horizontal_size;
    public int display_vertical_size;
    public ColorDescription colorDescription;

    public static class ColorDescription {
        int colour_primaries;
        int transfer_characteristics;
        int matrix_coefficients;

        public static ColorDescription read(BitReader in) {
            ColorDescription cd = new ColorDescription();
            cd.colour_primaries = in.readNBit(8);
            cd.transfer_characteristics = in.readNBit(8);
            cd.matrix_coefficients = in.readNBit(8);
            return cd;
        }

        public void write(BitWriter out) {
            out.writeNBit(colour_primaries, 8);
            out.writeNBit(transfer_characteristics, 8);
            out.writeNBit(matrix_coefficients, 8);
        }
    }

    public static SequenceDisplayExtension read(BitReader in) {
        SequenceDisplayExtension sde = new SequenceDisplayExtension();
        sde.video_format = in.readNBit(3);
        if (in.read1Bit() == 1) {
            sde.colorDescription = ColorDescription.read(in);
        }
        sde.display_horizontal_size = in.readNBit(14);
        in.read1Bit();
        sde.display_vertical_size = in.readNBit(14);

        return sde;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(SequenceHeader.Sequence_Display_Extension, 4);
        
        bw.writeNBit(video_format, 3);
        bw.write1Bit(colorDescription != null ? 1 : 0);
        if (colorDescription != null)
            colorDescription.write(bw);
        bw.writeNBit(display_horizontal_size, 14);
        bw.write1Bit(1); // verify this
        bw.writeNBit(display_vertical_size, 14);
        bw.flush();
    }
}
