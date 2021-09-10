package org.jcodec.codecs.vpx.vp8.enums;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public enum CompressMode {
    /*
     * mode -> (0)=Realtime/Live Encoding. This mode is optimized for realtim
     * encoding (for example, capturing a television signal or feed from a live
     * camera). ( speed setting controls how fast ) (1)=Good Quality Fast Encoding.
     * The encoder balances quality with the amount of time it takes to encode the
     * output. ( speed setting controls how fast ) (2)=One Pass - Best Quality. The
     * encoder places priority on the quality of the output over encoding speed. The
     * output is compressed at the highest possible quality. This option takes the
     * longest amount of time to encode.
     */

    REALTIME, GOODQUALITY, BESTQUALITY;
}
