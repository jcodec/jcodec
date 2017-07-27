package org.jcodec.scale.highbd;

import org.jcodec.common.model.PictureHiBD;


/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public interface TransformHiBD {
    public static enum Levels {
        STUDIO, PC
    };
    public void transform(PictureHiBD src, PictureHiBD dst);
}