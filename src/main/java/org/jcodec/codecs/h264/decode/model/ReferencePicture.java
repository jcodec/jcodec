package org.jcodec.codecs.h264.decode.model;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents reference picture
 * 
 * @author Jay Codec
 * 
 */
public class ReferencePicture {
    private Picture picture;
    private int picNum;
    private boolean longTerm;

    public ReferencePicture(Picture picture, int picNum, boolean longTerm) {
        this.picture = picture;
        this.picNum = picNum;
        this.longTerm = longTerm;
    }

    public Picture getPicture() {
        return picture;
    }

    public int getPicNum() {
        return picNum;
    }

    public boolean isLongTerm() {
        return longTerm;
    }
}