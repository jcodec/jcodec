package org.jcodec.player.filters;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Video output interface
 * 
 * @author The JCodec project
 * 
 */
public interface VideoOutput {

    void show(Picture8Bit pic, Rational rational);
    
    ColorSpace getColorSpace();

}
