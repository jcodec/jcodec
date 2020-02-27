package net.sourceforge.jaad.aac.syntax;

import org.jcodec.common.io.BitReader;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public class FIL extends Element implements SyntaxConstants {

    public static class DynamicRangeInfo {

        private static final int MAX_NBR_BANDS = 7;
        private final boolean[] excludeMask;
        private final boolean[] additionalExcludedChannels;
        private boolean pceTagPresent;
        private int pceInstanceTag;
        private int tagReservedBits;
        private boolean excludedChannelsPresent;
        private boolean bandsPresent;
        private int bandsIncrement, interpolationScheme;
        private int[] bandTop;
        private boolean progRefLevelPresent;
        private int progRefLevel, progRefLevelReservedBits;
        private boolean[] dynRngSgn;
        private int[] dynRngCtl;

        public DynamicRangeInfo() {
            excludeMask = new boolean[MAX_NBR_BANDS];
            additionalExcludedChannels = new boolean[MAX_NBR_BANDS];
        }
    }

    private static final int TYPE_FILL = 0;
    private static final int TYPE_FILL_DATA = 1;
    private static final int TYPE_EXT_DATA_ELEMENT = 2;
    private static final int TYPE_DYNAMIC_RANGE = 11;
    private static final int TYPE_SBR_DATA = 13;
    private static final int TYPE_SBR_DATA_CRC = 14;
    private final boolean downSampledSBR;
    private DynamicRangeInfo dri;

    public FIL(boolean downSampledSBR) {
        super();
        this.downSampledSBR = downSampledSBR;
    }

    public void decode(BitReader _in, Element prev, SampleFrequency sf, boolean sbrEnabled, boolean smallFrames)
            throws AACException {
        int count = _in.readNBit(4);
        if (count == 15)
            count += _in.readNBit(8) - 1;
        count *= 8; // convert to bits

        final int cpy = count;
        final int pos = _in.position();

        while (count > 0) {
            count = decodeExtensionPayload(_in, count, prev, sf, sbrEnabled, smallFrames);
        }

        final int pos2 = _in.position() - pos;
        final int bitsLeft = cpy - pos2;
        if (bitsLeft > 0)
            _in.skip(pos2);
        else if (bitsLeft < 0)
            throw new AACException("FIL element overread: " + bitsLeft);
    }

    private int decodeExtensionPayload(BitReader _in, int count, Element prev, SampleFrequency sf, boolean sbrEnabled,
            boolean smallFrames) throws AACException {
        final int type = _in.readNBit(4);
        int ret = count - 4;
        switch (type) {
        case TYPE_DYNAMIC_RANGE:
            ret = decodeDynamicRangeInfo(_in, ret);
            break;
        case TYPE_SBR_DATA:
        case TYPE_SBR_DATA_CRC:
            if (sbrEnabled) {
                if (prev instanceof SCE_LFE || prev instanceof CPE || prev instanceof CCE) {
                    prev.decodeSBR(_in, sf, ret, (prev instanceof CPE), (type == TYPE_SBR_DATA_CRC), downSampledSBR,
                            smallFrames);
                    ret = 0;
                    break;
                } else
                    throw new AACException("SBR applied on unexpected element: " + prev);
            } else {
                _in.skip(ret);
                ret = 0;
            }
        case TYPE_FILL:
        case TYPE_FILL_DATA:
        case TYPE_EXT_DATA_ELEMENT:
        default:
            _in.skip(ret);
            ret = 0;
            break;
        }
        return ret;
    }

    private int decodeDynamicRangeInfo(BitReader _in, int count) throws AACException {
        if (dri == null)
            dri = new DynamicRangeInfo();
        int ret = count;

        int bandCount = 1;

        // pce tag
        if (dri.pceTagPresent = _in.readBool()) {
            dri.pceInstanceTag = _in.readNBit(4);
            dri.tagReservedBits = _in.readNBit(4);
        }

        // excluded channels
        if (dri.excludedChannelsPresent = _in.readBool()) {
            ret -= decodeExcludedChannels(_in);
        }

        // bands
        if (dri.bandsPresent = _in.readBool()) {
            dri.bandsIncrement = _in.readNBit(4);
            dri.interpolationScheme = _in.readNBit(4);
            ret -= 8;
            bandCount += dri.bandsIncrement;
            dri.bandTop = new int[bandCount];
            for (int i = 0; i < bandCount; i++) {
                dri.bandTop[i] = _in.readNBit(8);
                ret -= 8;
            }
        }

        // prog ref level
        if (dri.progRefLevelPresent = _in.readBool()) {
            dri.progRefLevel = _in.readNBit(7);
            dri.progRefLevelReservedBits = _in.readNBit(1);
            ret -= 8;
        }

        dri.dynRngSgn = new boolean[bandCount];
        dri.dynRngCtl = new int[bandCount];
        for (int i = 0; i < bandCount; i++) {
            dri.dynRngSgn[i] = _in.readBool();
            dri.dynRngCtl[i] = _in.readNBit(7);
            ret -= 8;
        }
        return ret;
    }

    private int decodeExcludedChannels(BitReader _in) throws AACException {
        int i;
        int exclChs = 0;

        do {
            for (i = 0; i < 7; i++) {
                dri.excludeMask[exclChs] = _in.readBool();
                exclChs++;
            }
        } while (exclChs < 57 && _in.readBool());

        return (exclChs / 7) * 8;
    }
}
