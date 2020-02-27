package net.sourceforge.jaad.aac.syntax;

import org.jcodec.common.io.BitReader;

import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public class DSE extends Element {

    private byte[] dataStreamBytes;

    public DSE() {
        super();
    }

    public void decode(BitReader _in) throws AACException {
        final boolean byteAlign = _in.readBool();
        int count = _in.readNBit(8);
        if (count == 255)
            count += _in.readNBit(8);

        if (byteAlign)
            _in.align();

        dataStreamBytes = new byte[count];
        for (int i = 0; i < count; i++) {
            dataStreamBytes[i] = (byte) _in.readNBit(8);
        }
    }
}
