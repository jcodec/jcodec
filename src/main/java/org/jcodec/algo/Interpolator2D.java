package org.jcodec.algo;

import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Plane;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface Interpolator2D {

    void interpolate(Plane in, Plane out);

    void interpolate(Picture in, Picture out);
}
