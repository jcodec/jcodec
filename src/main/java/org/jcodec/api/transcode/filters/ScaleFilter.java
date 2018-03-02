package org.jcodec.api.transcode.filters;

import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.scale.BaseResampler;
import org.jcodec.scale.LanczosResampler;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Scales image to a different size.
 * 
 * @author The JCodec project
 * 
 */
public class ScaleFilter implements Filter {
    private BaseResampler resampler;
    private ColorSpace currentColor;
    private Size currentSize;
    private Size targetSize;
    private int width;
    private int height;

    public ScaleFilter(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        Size pictureSize = picture.getSize();
        if (resampler == null || currentColor != picture.getColor() || !pictureSize.equals(currentSize)) {
            currentColor = picture.getColor();
            currentSize = picture.getSize();
            targetSize = new Size(width & currentColor.getWidthMask(), height & currentColor.getHeightMask());
            resampler = new LanczosResampler(currentSize, targetSize);
        }

        LoanerPicture dest = store.getPicture(targetSize.getWidth(), targetSize.getHeight(), currentColor);

        resampler.resample(picture, dest.getPicture());

        return dest;
    }

    @Override
    public ColorSpace getInputColor() {
        return ColorSpace.ANY_PLANAR;
    }

    @Override
    public ColorSpace getOutputColor() {
        return ColorSpace.SAME;
    }
}
