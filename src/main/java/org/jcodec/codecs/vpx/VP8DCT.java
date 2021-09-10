package org.jcodec.codecs.vpx;

import org.jcodec.codecs.vpx.vp8.DCT;
import org.jcodec.codecs.vpx.vp8.IDCTllm;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @see http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/de//pubs/archive/37073.pdf
 * @see http://jpegclub.org/jidctred/
 * @see http://www3.matapp.unimib.it/corsi-2007-2008/matematica/istituzioni-di-analisi-numerica/jpeg/papers/11-multiplications.pdf
 * @see http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/de//pubs/archive/37073.pdf
 * 
 *      <pre>
 *      </pre>
 * 
 * @author The JCodec project
 */
public class VP8DCT {

    public static short[] decodeDCT(short input[]) {
        FullAccessIntArrPointer out = IDCTllm.vp8_short_idct4x4NoAdd(new PositionableIntArrPointer(input, 0));
        short[] output = new short[16];
        out.memcopyout(0, output, 0, output.length);
        return output;

    }

    public static short[] encodeDCT(short[] input) {
        short[] output = new short[input.length];
        DCT.fdct4x4(new PositionableIntArrPointer(input, 0), FullAccessIntArrPointer.toPointer(output), 8);
        return output;
    }

    public static short[] decodeWHT(short[] input) {
        short[] outputLarge = new short[256];
        IDCTllm.vp8_short_inv_walsh4x4(new PositionableIntArrPointer(input, 0),
                FullAccessIntArrPointer.toPointer(outputLarge));

        short[] output = new short[16];
        for (int i = 0; i < output.length; i++) {
            output[i] = outputLarge[i << 4];
        }
        return output;

    }

    public static short[] encodeWHT(short[] input) {
        short[] output = new short[input.length];
        DCT.walsh4x4(new PositionableIntArrPointer(input, 0), FullAccessIntArrPointer.toPointer(output), 8);
        return output;
    }
}