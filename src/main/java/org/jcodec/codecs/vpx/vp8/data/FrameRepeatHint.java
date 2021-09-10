package org.jcodec.codecs.vpx.vp8.data;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * @author The JCodec project
 * 
 */
public class FrameRepeatHint {
    private boolean frameIsRepeat = false;

    public boolean isFrameRepeat() {
        return frameIsRepeat;
    }

    public void setFrameIsRepeat(boolean frameIsRepeat) {
        this.frameIsRepeat = frameIsRepeat;
    }

}
