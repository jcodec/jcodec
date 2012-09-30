package org.jcodec.codecs.aac.blocks;

import java.io.IOException;

import org.jcodec.common.io.InBits;

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

    public void parse(InBits in) throws IOException {

        // int i, ret, common_window, ms_present = 0;
        //
        int common_window = in.read1Bit();
        if (common_window != 0) {
            parseICSInfo(in);
            // i = cpe->ch[1].ics.use_kb_window[0];
            // cpe->ch[1].ics = cpe->ch[0].ics;
            // cpe->ch[1].ics.use_kb_window[1] = i;
            // if (cpe->ch[1].ics.predictor_present && (ac->m4ac.object_type !=
            // AOT_AAC_MAIN))
            // if ((cpe->ch[1].ics.ltp.present = get_bits(gb, 1)))
            // decode_ltp(ac, &cpe->ch[1].ics.ltp, gb, cpe->ch[1].ics.max_sfb);
            int ms_present = in.readNBit(2);
            if (ms_present == 3) {
                throw new RuntimeException("ms_present = 3 is reserved.");
            } else if (ms_present != 0)
                decodeMidSideStereo(in, ms_present, 0, 0);
        }
        BlockICS ics1 = new BlockICS();
        ics1.parse(in);
        BlockICS ics2 = new BlockICS();
        ics2.parse(in);

    }

    private void decodeMidSideStereo(InBits in, int ms_present, int numWindowGroups, int maxSfb) throws IOException {
        if (ms_present == 1) {
            for (int idx = 0; idx < numWindowGroups * maxSfb; idx++)
                ms_mask[idx] = in.read1Bit();
        }
    }
}