package org.jcodec.codecs.h264.decode.model;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class NonIDRFrame extends DecodedFrame {
    private RefPicMarking marking;

    public NonIDRFrame(Picture picture, NALUnit nu, int frameId, int poc, RefPicMarking marking) {
        super(picture, nu, frameId, poc);
        this.marking = marking;
    }

    public RefPicMarking getMarking() {
        return marking;
    }
}
