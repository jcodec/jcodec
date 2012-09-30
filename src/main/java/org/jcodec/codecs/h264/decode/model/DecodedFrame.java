package org.jcodec.codecs.h264.decode.model;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A base class for IDR and NonIDR decoded frames. Contains a picture for
 * decoded frame as well as auxilary information needed for reference picture
 * management process.
 * 
 * @author Jay Codec
 * 
 */
public abstract class DecodedFrame {
    private Picture picture;
    private NALUnit nu;
    private int frameNum;
    private int poc;

    public DecodedFrame(Picture picture, NALUnit nu, int frameNum, int poc) {
        this.picture = picture;
        this.nu = nu;
        this.frameNum = frameNum;
        this.poc = poc;
    }

    public Picture getPicture() {
        return picture;
    }

    public NALUnit getNU() {
        return nu;
    }

    public int getFrameNum() {
        return frameNum;
    }

    public int getPOC() {
        return poc;
    }
}