package org.jcodec.codecs.common.biari;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * H264 CABAC M-Coder ( decoder module )
 * 
 * @author Jay Codec
 * 
 */
public class MDecoder {

    private InputStream in;
    private int range;
    private int code;
    private int nBitsPending;

    public MDecoder(InputStream in) throws IOException {
        this.in = in;
        range = 510;

        initCodeRegister();
    }

    /**
     * Initializes code register. Loads 9 bits from the stream into working area
     * of code register ( bits 8 - 16) leaving 7 bits in the pending area of
     * code register (bits 0 - 7)
     * 
     * @throws IOException
     */
    private void initCodeRegister() throws IOException {
        readOneByte();
        if (nBitsPending != 8)
            throw new IOException("Empty stream");
        code <<= 8;
        readOneByte();
        code <<= 1;
        nBitsPending -= 9;
    }

    private void readOneByte() throws IOException {
        int b = in.read();
        if (b != -1) {
            code |= b;
            nBitsPending += 8;
        }
    }

    /**
     * Decodes one bin from arithmetice code word
     * 
     * @param cm
     * @return
     * @throws IOException
     */
    public int decodeBin(Context cm) throws IOException {
        int bin;

        int qIdx = (range >> 6) & 0x3;
        int rLPS = MConst.rangeLPS[qIdx][cm.getState()];
        range -= rLPS;
        int rs8 = range << 8;

        if (code < rs8) {
            // MPS
            if (cm.getState() < 62)
                cm.setState(cm.getState() + 1);

            renormalize();

            bin = cm.getMps();
        } else {
            // LPS
            range = rLPS;
            code -= rs8;

            renormalize();

            bin = 1 - cm.getMps();

            if (cm.getState() == 0)
                cm.setMps(1 - cm.getMps());

            cm.setState(MConst.transitLPS[cm.getState()]);
        }

        return bin;
    }

    /**
     * Special decoding process for 'end of slice' flag. Uses probability state
     * 63.
     * 
     * @param cm
     * @return
     * @throws IOException
     */
    public int decodeFinalBin() throws IOException {
        range -= 2;

        if (code < (range << 8)) {
            renormalize();
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Special decoding process for symbols with uniform distribution
     * 
     * @return
     * @throws IOException
     */
    public int decodeBinBypass() throws IOException {
        code <<= 1;

        --nBitsPending;
        if (nBitsPending <= 0)
            readOneByte();

        int tmp = code - (range << 8);
        if (tmp < 0) {
            return 0;
        } else {
            code = tmp;
            return 1;
        }
    }

    /**
     * Shifts the current interval to either 1/2 or 0 (code = (code << 1) &
     * 0x1ffff) and scales it by 2 (range << 1).
     * 
     * Reads new byte from the input stream into code value if there are no more
     * bits pending
     * 
     * @throws IOException
     */
    private void renormalize() throws IOException {
        while (range < 256) {
            range <<= 1;
            code <<= 1;
            code &= 0x1ffff;

            --nBitsPending;
            if (nBitsPending <= 0)
                readOneByte();
        }
    }
}