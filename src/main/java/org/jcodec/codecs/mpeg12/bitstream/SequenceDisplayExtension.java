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
public class SequenceDisplayExtension {
    public int video_format;
    public int display_horizontal_size;
    public int display_vertical_size;
    public ColorDescription colorDescription;

    public static class ColorDescription {
        int colour_primaries;
        int transfer_characteristics;
        int matrix_coefficients;

        public static ColorDescription read(BitstreamReaderBB in) {
            ColorDescription cd = new ColorDescription();
            cd.colour_primaries = in.readNBit(8);
            cd.transfer_characteristics = in.readNBit(8);
            cd.matrix_coefficients = in.readNBit(8);
            return cd;
        }

        public void write(OutBits out) throws IOException {
            out.writeNBit(colour_primaries, 8);
            out.writeNBit(transfer_characteristics, 8);
            out.writeNBit(matrix_coefficients, 8);
        }
    }

    public static SequenceDisplayExtension read(BitstreamReaderBB in) {
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

    public void write(OutBits out) throws IOException {
        out.writeNBit(video_format, 3);
        out.write1Bit(colorDescription != null ? 1 : 0);
        if (colorDescription != null)
            colorDescription.write(out);
        out.writeNBit(display_horizontal_size, 14);
        out.write1Bit(1); // verify this
        out.writeNBit(display_vertical_size, 14);
    }
}
