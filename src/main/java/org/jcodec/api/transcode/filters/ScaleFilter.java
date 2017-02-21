package org.jcodec.api.transcode.filters;

import org.jcodec.common.model.Picture8Bit;
import org.jcodec.api.transcode.Transcoder.Filter;
import org.jcodec.api.transcode.Transcoder.PixelStore;

public class ScaleFilter implements Filter {

    @Override
    public Picture8Bit filter(Picture8Bit picture, PixelStore store) {
        return picture;
    }
}
