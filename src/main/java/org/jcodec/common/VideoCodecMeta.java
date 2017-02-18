package org.jcodec.common;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

public class VideoCodecMeta {
    private Size size;
    private Rational pixelAspectRatio;
    private ColorSpace color;

    public VideoCodecMeta(Size size, ColorSpace color) {
        this.size = size;
        this.color = color;
    }

    public Size getSize() {
        return size;
    }

    public ColorSpace getColor() {
        return color;
    }

    public Rational getPixelAspectRatio() {
        return pixelAspectRatio;
    }

    public void setPixelAspectRatio(Rational pixelAspectRatio) {
        this.pixelAspectRatio = pixelAspectRatio;
    }
}
