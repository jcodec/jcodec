package org.jcodec.api.transcode;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

public class PixelStoreImpl implements PixelStore {
    private List<Picture8Bit> buffers = new ArrayList<Picture8Bit>();

    @Override
    public LoanerPicture getPicture(int width, int height, ColorSpace color) {
        for (Picture8Bit picture8Bit : buffers) {
//            Logger.debug("Trying");
            if (picture8Bit.getWidth() == width && picture8Bit.getHeight() == height
                    && picture8Bit.getColor() == color) {
//                Logger.debug("Reusing");
                buffers.remove(picture8Bit);
                return new LoanerPicture(picture8Bit, 1);
            } /*else {
                if(picture8Bit.getWidth() != width)
                    Logger.debug("width");
                if(picture8Bit.getHeight() != height)
                    Logger.debug("height");
                if ( picture8Bit.getColor() != color)
                    Logger.debug("color " + picture8Bit.getColor() + " " + color);
            }*/
        }
//        Logger.debug("Creating picture");
        return new LoanerPicture(Picture8Bit.create(width, height, color), 1);
    }

    @Override
    public void putBack(LoanerPicture frame) {
        frame.decRefCnt();
        if (frame.unused()) {
            Picture8Bit pixels = frame.getPicture();
            pixels.setCrop(null);
//            Logger.debug("Returning picture");
            buffers.add(pixels);
        } /*else {
            Logger.debug("Picture is used");
        }*/
    }

    @Override
    public void retake(LoanerPicture frame) {
        frame.incRefCnt();
    }
}