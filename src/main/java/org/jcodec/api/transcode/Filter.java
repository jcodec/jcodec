package org.jcodec.api.transcode;

import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

/**
 * Filters the decoded image before it gets to encoder.
 * 
 * @author stan
 */
public interface Filter {
    LoanerPicture filter(Picture8Bit picture, PixelStore store);
    
    /**
     * The color space that this filter supports on the input. null indicates any color space is taken.
     * @return
     */
    ColorSpace getInputColor();
    
    ColorSpace getOutputColor();
}