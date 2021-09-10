package org.jcodec.codecs.vpx.vp8.intrapred;

import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;

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
public class DC128Predictor extends SingleValPredictor {

    public DC128Predictor(int bs) {
        super(bs);
    }

    @Override
    protected short calcSingleValue(ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        return VP8Encoder.INT_TO_BYTE_OFFSET;
    }

}
