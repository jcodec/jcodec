package org.jcodec.api.transcode;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

public class PixelStoreImpl implements PixelStore {
    private List<Picture8Bit> buffers = new ArrayList<Picture8Bit>();

    @Override
    public Picture8Bit getPicture(int width, int height, ColorSpace color) {
        for (Picture8Bit picture8Bit : buffers) {
            if (picture8Bit.getWidth() == width && picture8Bit.getHeight() == height
                    && picture8Bit.getColor() == color) {
                buffers.remove(picture8Bit);
                return picture8Bit;
            }
        }
        return Picture8Bit.create(width, height, color);
    }

    @Override
    public void putBack(Picture8Bit frame) {
        frame.setCrop(null);
        buffers.add(frame);
    }
}