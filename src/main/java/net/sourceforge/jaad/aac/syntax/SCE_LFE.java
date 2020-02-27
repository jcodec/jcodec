package net.sourceforge.jaad.aac.syntax;

import org.jcodec.common.io.BitReader;

import net.sourceforge.jaad.aac.AACDecoderConfig;
import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public class SCE_LFE extends Element {

    private final ICStream ics;

    public SCE_LFE(int frameLength) {
        super();
        ics = new ICStream(frameLength);
    }

    public void decode(BitReader _in, AACDecoderConfig conf) throws AACException {
        readElementInstanceTag(_in);
        ics.decode(_in, false, conf);
    }

    public ICStream getICStream() {
        return ics;
    }
}
