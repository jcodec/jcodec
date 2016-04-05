package org.jcodec.common.model;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Frame {
    private Picture pic;
    private RationalLarge pts;
    private RationalLarge duration;
    private Rational pixelAspect;
    private TapeTimecode tapeTimecode;
    private int frameNo;
    private List<String> messages;

    public Frame(Picture pic, RationalLarge pts, RationalLarge duration, Rational pixelAspect, int frameNo,
            TapeTimecode tapeTimecode, List<String> messages) {
        this.pic = pic;
        this.pts = pts;
        this.duration = duration;
        this.pixelAspect = pixelAspect;
        this.tapeTimecode = tapeTimecode;
        this.frameNo = frameNo;
        this.messages = messages;
    }

    public Picture getPic() {
        return pic;
    }

    public RationalLarge getPts() {
        return pts;
    }

    public RationalLarge getDuration() {
        return duration;
    }

    public Rational getPixelAspect() {
        return pixelAspect;
    }

    public TapeTimecode getTapeTimecode() {
        return tapeTimecode;
    }

    public int getFrameNo() {
        return frameNo;
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean isAvailable() {
        return true;
    }
}
