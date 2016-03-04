package org.jcodec.scale;

import org.jcodec.common.model.Picture8Bit;


/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public interface Transform8Bit {
    public static enum Levels {
        STUDIO, PC
    };
    public void transform(Picture8Bit src, Picture8Bit dst);
}