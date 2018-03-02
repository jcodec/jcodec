package org.jcodec.api.transcode.filters;

import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Color transform filter.
 * 
 * @author Stanislav Vitvitskyy
 */
public class ColorTransformFilter implements Filter {
    private Transform transform;
    private ColorSpace outputColor;

    public ColorTransformFilter(ColorSpace outputColor) {
        this.outputColor = outputColor;
    }

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        if (transform == null) {
            transform = ColorUtil.getTransform(picture.getColor(), outputColor);
            Logger.debug("Creating transform: " + transform);
        }
        LoanerPicture outFrame = store.getPicture(picture.getWidth(), picture.getHeight(), outputColor);
        outFrame.getPicture().setCrop(picture.getCrop());
        transform.transform(picture, outFrame.getPicture());
        return outFrame;
    }

    @Override
    public ColorSpace getInputColor() {
        return ColorSpace.ANY_PLANAR;
    }

    @Override
    public ColorSpace getOutputColor() {
        return outputColor;
    }
}