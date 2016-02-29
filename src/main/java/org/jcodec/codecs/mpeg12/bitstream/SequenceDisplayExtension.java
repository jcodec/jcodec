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
    public static final int Sequence_Display_Extension = 0x2;

    public static class ColorDescription {
        int colour_primaries;
        int transfer_characteristics;
        int matrix_coefficients;

        public static ColorDescription read(BitReader _in) {
            ColorDescription cd = new ColorDescription();
            cd.colour_primaries = _in.readNBit(8);
            cd.transfer_characteristics = _in.readNBit(8);
            cd.matrix_coefficients = _in.readNBit(8);
            return cd;
        }

        public void write(BitWriter out) {
            out.writeNBit(colour_primaries, 8);
            out.writeNBit(transfer_characteristics, 8);
            out.writeNBit(matrix_coefficients, 8);
        }
    }

    public static SequenceDisplayExtension read(BitReader _in) {
        SequenceDisplayExtension sde = new SequenceDisplayExtension();
        sde.video_format = _in.readNBit(3);
        if (_in.read1Bit() == 1) {
            sde.colorDescription = ColorDescription.read(_in);
        }
        sde.display_horizontal_size = _in.readNBit(14);
        _in.read1Bit();
        sde.display_vertical_size = _in.readNBit(14);

        return sde;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(SequenceDisplayExtension.Sequence_Display_Extension, 4);
        
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
