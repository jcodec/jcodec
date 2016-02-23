package org.jcodec.codecs.aac.blocks;

import org.jcodec.common.io.BitReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Channel pair element; reference: table 4.4.
 * 
 * @author The JCodec project
 * 
 */
public class BlockCPE extends BlockICS {

    private int[] ms_mask;

    public void parse(BitReader _in) {

        // int i, ret, common_window, ms_present = 0;
        //
        int common_window = _in.read1Bit();
        if (common_window != 0) {
            parseICSInfo(_in);
            // i = cpe->ch[1].ics.use_kb_window[0];
            // cpe->ch[1].ics = cpe->ch[0].ics;
            // cpe->ch[1].ics.use_kb_window[1] = i;
            // if (cpe->ch[1].ics.predictor_present && (ac->m4ac.object_type !=
            // AOT_AAC_MAIN))
            // if ((cpe->ch[1].ics.ltp.present = get_bits(gb, 1)))
            // decode_ltp(ac, &cpe->ch[1].ics.ltp, gb, cpe->ch[1].ics.max_sfb);
            int ms_present = _in.readNBit(2);
            if (ms_present == 3) {
                throw new RuntimeException("ms_present = 3 is reserved.");
            } else if (ms_present != 0)
                decodeMidSideStereo(_in, ms_present, 0, 0);
        }
        BlockICS ics1 = new BlockICS();
        ics1.parse(_in);
        BlockICS ics2 = new BlockICS();
        ics2.parse(_in);

    }

    private void decodeMidSideStereo(BitReader _in, int ms_present, int numWindowGroups, int maxSfb) {
        if (ms_present == 1) {
            for (int idx = 0; idx < numWindowGroups * maxSfb; idx++)
                ms_mask[idx] = _in.read1Bit();
        }
    }
}