package org.jcodec.api.transcode.filters;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;

public class ScaleFilter implements Filter {

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        return null;
    }

    @Override
    public ColorSpace getInputColor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ColorSpace getOutputColor() {
        // TODO Auto-generated method stub
        return null;
    }
}
