package org.jcodec.codecs.h264.decode;

import java.io.IOException;

import org.jcodec.codecs.h264.decode.dpb.DecodedPicture;
import org.jcodec.codecs.h264.io.model.CodedPicture;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Only keeps primary picture. Ignores everything else. No error correction is
 * performed.
 * 
 * @author Jay Codec
 * 
 */
public class IgnorentErrorResilence implements ErrorResilence {
    private PictureDecoder picDecoder;

    private SeqParameterSet curSPS;
    private PictureParameterSet curPPS;

    public Picture decodeAccessUnit(AccessUnitReader accessUnit, DecodedPicture[] references) throws IOException {
        PictureParameterSet newPPS = accessUnit.getPPS();
        SeqParameterSet newSPS = accessUnit.getSPS();

        if (curSPS == null || curPPS == null || curPPS != newPPS || curSPS != newSPS) {
            curSPS = newSPS;
            curPPS = newPPS;

            picDecoder = new PictureDecoder(curSPS, curPPS);
        }

        CodedPicture primary = new CodedPicture(accessUnit.readOnePicture());

        return picDecoder.decodePicture(primary, accessUnit.getMap(), references);
    }
}