package org.jcodec.api.transcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

public class PixelStoreImpl implements PixelStore {
    private Random rnd = new Random();
    private int bound;
    private List<Picture> buffers;

    public PixelStoreImpl() {
        buffers = new ArrayList<Picture>();
        bound = 10;
    }

    public int getBound() {
        return bound;
    }

    public void setBound(int bound) {
        this.bound = bound;
    }

    @Override
    public LoanerPicture getPicture(int width, int height, ColorSpace color) {
        for (Picture picture : buffers) {
            if (picture.getWidth() == width && picture.getHeight() == height && picture.getColor() == color) {
                buffers.remove(picture);
                return new LoanerPicture(picture, 1);
            }
        }
        return new LoanerPicture(Picture.create(width, height, color), 1);
    }

    @Override
    public void putBack(LoanerPicture frame) {
        frame.decRefCnt();
        if (frame.unused()) {
            Picture pixels = frame.getPicture();
            pixels.setCrop(null);
            if (buffers.size() == bound) {
                buffers.set(rnd.nextInt(bound), pixels);
            } else {
                buffers.add(pixels);
            }
        }
    }

    @Override
    public void retake(LoanerPicture frame) {
        frame.incRefCnt();
    }
}