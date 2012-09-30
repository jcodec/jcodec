package org.jcodec.codecs.h264.decode.dpb;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents a picture from a decoded picture buffer
 * 
 * @author Jay Codec
 * 
 */
public class DecodedPicture {
    private Picture picture;
    private int poc;
    private boolean display;
    private boolean ref;
    private int frameNum;
    private int ltPicId;
    private boolean longTerm;
    private boolean mmco5;

    public DecodedPicture(Picture picture, int poc, boolean display, boolean ref, int frameNum, boolean longTerm,
            boolean mmco5) {
        this.picture = picture;
        this.poc = poc;
        this.display = display;
        this.ref = ref;
        this.frameNum = frameNum;
        this.longTerm = longTerm;
        this.mmco5 = mmco5;
    }

    public Picture getPicture() {
        return picture;
    }

    public int getPoc() {
        return poc;
    }

    public boolean isDisplay() {
        return display;
    }

    public boolean isRef() {
        return ref;
    }

    public int getFrameNum() {
        return frameNum;
    }

    public int getLtPicId() {
        return ltPicId;
    }

    public boolean isLongTerm() {
        return longTerm;
    }

    public void unreference() {
        this.ref = false;
    }

    public void setDisplayed() {
        this.display = false;
    }

    public void makeLongTerm(int longNo) {
        this.longTerm = true;
        this.ltPicId = longNo;
    }

    public void resetFrameNum() {
        this.frameNum = 0;
    }

    public boolean hasMMCO5() {
        return mmco5;
    }
}