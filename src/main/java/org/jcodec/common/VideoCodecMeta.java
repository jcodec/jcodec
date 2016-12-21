package org.jcodec.common;

import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

public class VideoCodecMeta {
    private Size size;
    private Rational pixelAspectRatio;

    public VideoCodecMeta(Size size) {
        this.size = size;
    }

    public Size getSize() {
        return size;
    }

    public Rational getPixelAspectRatio() {
        return pixelAspectRatio;
    }

    public void setPixelAspectRatio(Rational pixelAspectRatio) {
        this.pixelAspectRatio = pixelAspectRatio;
    }
}
