package org.jcodec.codecs.aac.blocks;

import static org.jcodec.codecs.aac.BlockType.TYPE_CPE;
import static org.jcodec.codecs.aac.BlockType.TYPE_SCE;
import static org.jcodec.codecs.aac.blocks.BlockCCE.CouplingPoint.AFTER_IMDCT;
import static org.jcodec.codecs.aac.blocks.BlockICS.BandType.ZERO_BT;

import org.jcodec.codecs.aac.BlockType;
import org.jcodec.codecs.aac.blocks.BlockICS.BandType;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Coupling_channel_element; reference: table 4.8.
 * 
 * @author The JCodec project
 * 
 */
public class BlockCCE extends Block {

    private int coupling_point;
    private int num_coupled;
    private BlockType[] type;
    private int[] id_select;
    private int[] ch_select;
    private int sign;
    private Object scale;
    private Object[] cce_scale;
    private BlockICS blockICS;
    private BandType[] bandType;
    static VLC vlc;

    static {
        vlc = new VLC(AACTab.ff_aac_scalefactor_code, AACTab.ff_aac_scalefactor_bits);
    }

    public BlockCCE(BandType[] bandType) {
        this.bandType = bandType;
    }

    public void parse(BitReader _in) {
        int num_gain = 0;
        coupling_point = 2 * _in.read1Bit();
        num_coupled = _in.readNBit(3);
        for (int c = 0; c <= num_coupled; c++) {
            num_gain++;
            type[c] = _in.read1Bit() != 0 ? TYPE_CPE : TYPE_SCE;
            id_select[c] = _in.readNBit(4);
            if (type[c] == TYPE_CPE) {
                ch_select[c] = _in.readNBit(2);
                if (ch_select[c] == 3)
                    num_gain++;
            } else
                ch_select[c] = 2;
        }
        coupling_point += _in.read1Bit() | (coupling_point >> 1);

        sign = _in.read1Bit();
        scale = cce_scale[_in.readNBit(2)];

        blockICS = new BlockICS();
        blockICS.parse(_in);

        for (int c = 0; c < num_gain; c++) {
            int idx = 0;
            int cge = 1;
            int gain = 0;
            if (c != 0) {
                cge = coupling_point == AFTER_IMDCT.ordinal() ? 1 : _in.read1Bit();
                gain = cge != 0 ? vlc.readVLC(_in) - 60 : 0;
                // gain_cache = powf(scale, -gain);
            }
            if (coupling_point != AFTER_IMDCT.ordinal()) {
                for (int g = 0; g < blockICS.num_window_groups; g++) {
                    for (int sfb = 0; sfb < blockICS.maxSfb; sfb++, idx++) {
                        if (bandType[idx] != ZERO_BT) {
                            if (cge == 0) {
                                int t = vlc.readVLC(_in) - 60;
                            }
                        }
                    }
                }
            }
        }
    }

    enum CouplingPoint {
        BEFORE_TNS, BETWEEN_TNS_AND_IMDCT, UNDEF, AFTER_IMDCT,
    };
}