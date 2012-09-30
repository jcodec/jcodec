package org.jcodec.codecs.h264.decode;

import java.io.IOException;

import org.jcodec.codecs.h264.decode.dpb.DecodedPicture;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public interface ErrorResilence {

    Picture decodeAccessUnit(AccessUnitReader accessUnit, DecodedPicture[] references) throws IOException;

}
