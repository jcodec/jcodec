package org.jcodec.codecs.h264.decode.model;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains picture of decoded IDR frame. Also contains auxilary information
 * needed for correct reference picture management.
 * 
 * @author Jay Codec
 * 
 */
public class IDRFrame extends DecodedFrame {

    private RefPicMarkingIDR marking;

    public IDRFrame(Picture picture, NALUnit nu, int frameId, int poc, RefPicMarkingIDR marking) {
        super(picture, nu, frameId, poc);
        this.marking = marking;
    }

    public RefPicMarkingIDR getMarking() {
        return marking;
    }
}
