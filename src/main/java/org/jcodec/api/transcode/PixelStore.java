package org.jcodec.api.transcode;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

public interface PixelStore {
    Picture8Bit getPicture(int width, int height, ColorSpace color);

    void putBack(Picture8Bit frame);
}