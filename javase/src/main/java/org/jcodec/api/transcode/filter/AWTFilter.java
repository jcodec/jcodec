package org.jcodec.api.transcode.filter;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.image.BufferedImage;

import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.javase.scale.AWTUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Stanislav Vitvitskiy
 * 
 */
public abstract class AWTFilter implements Filter {
    @Override
    public ColorSpace getOutputColor() {
        return ColorSpace.RGB;
    }

    @Override
    public ColorSpace getInputColor() {
        return ColorSpace.RGB;
    }

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        BufferedImage rgb = AWTUtil.toBufferedImage(picture);
        
        BufferedImage out = filterBufferedImage(rgb);

        LoanerPicture outRGB = store.getPicture(out.getWidth(), out.getHeight(), RGB);
        AWTUtil.fromBufferedImage(rgb, outRGB.getPicture());

        return outRGB;
    }

    protected abstract BufferedImage filterBufferedImage(BufferedImage rgb);
}