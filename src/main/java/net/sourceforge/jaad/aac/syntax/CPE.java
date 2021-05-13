package net.sourceforge.jaad.aac.syntax;

import java.util.Arrays;

import org.jcodec.common.io.BitReader;

import net.sourceforge.jaad.aac.AACDecoderConfig;
import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.tools.MSMask;
import static net.sourceforge.jaad.aac.syntax.SyntaxConstants.*;
/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public class CPE extends Element {

    private MSMask msMask;
    private boolean[] msUsed;
    private boolean commonWindow;
    ICStream icsL, icsR;

    public CPE(int frameLength) {
        super();
        msUsed = new boolean[MAX_MS_MASK];
        icsL = new ICStream(frameLength);
        icsR = new ICStream(frameLength);
    }

    public void decode(BitReader _in, AACDecoderConfig conf) throws AACException {
        final Profile profile = conf.getProfile();
        final SampleFrequency sf = conf.getSampleFrequency();
        if (sf.equals(SampleFrequency.SAMPLE_FREQUENCY_NONE))
            throw new AACException("invalid sample frequency");

        readElementInstanceTag(_in);

        commonWindow = _in.readBool();
        final ICSInfo info = icsL.getInfo();
        if (commonWindow) {
            info.decode(_in, conf, commonWindow);
            icsR.getInfo().setData(info);

            msMask = CPE.msMaskFromInt(_in.readNBit(2));
            if (msMask.equals(MSMask.TYPE_USED)) {
                final int maxSFB = info.getMaxSFB();
                final int windowGroupCount = info.getWindowGroupCount();

                for (int idx = 0; idx < windowGroupCount * maxSFB; idx++) {
                    msUsed[idx] = _in.readBool();
                }
            } else if (msMask.equals(MSMask.TYPE_ALL_1))
                Arrays.fill(msUsed, true);
            else if (msMask.equals(MSMask.TYPE_ALL_0))
                Arrays.fill(msUsed, false);
            else
                throw new AACException("reserved MS mask type used");
        } else {
            msMask = MSMask.TYPE_ALL_0;
            Arrays.fill(msUsed, false);
        }

        if (profile.isErrorResilientProfile() && (info.isLTPrediction1Present())) {
            info.ltpData2Present = _in.readBool();
            if (info.ltpData2Present)
                info.getLTPrediction2().decode(_in, info, profile);
        }

        icsL.decode(_in, commonWindow, conf);
        icsR.decode(_in, commonWindow, conf);
    }

    public ICStream getLeftChannel() {
        return icsL;
    }

    public ICStream getRightChannel() {
        return icsR;
    }

    public MSMask getMSMask() {
        return msMask;
    }

    public boolean isMSUsed(int off) {
        return msUsed[off];
    }

    public boolean isMSMaskPresent() {
        return !msMask.equals(MSMask.TYPE_ALL_0);
    }

    public boolean isCommonWindow() {
        return commonWindow;
    }

    public static MSMask msMaskFromInt(int i) throws AACException {
        MSMask[] values = MSMask.values();
        if (i >= values.length) {
            throw new AACException("unknown MS mask type");
        }
        return values[i];
    }
}
