package org.jcodec.api.transcode.filters;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;

public class ScaleFilter implements Filter {

    @Override
    public Picture8Bit filter(Picture8Bit picture, PixelStore store) {
        return picture;
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
