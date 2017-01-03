package org.jcodec.samples.transcode.filters;

import org.jcodec.common.model.Picture8Bit;
import org.jcodec.samples.transcode.Transcoder.Filter;
import org.jcodec.samples.transcode.Transcoder.PixelStore;

public class CropFilter implements Filter {

    @Override
    public Picture8Bit filter(Picture8Bit picture, PixelStore store) {
        return picture;
    }
}
