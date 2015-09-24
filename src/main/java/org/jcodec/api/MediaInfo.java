package org.jcodec.api;

import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MediaInfo {
    private Size dim;

    public MediaInfo(Size dim) {
        super();
        this.dim = dim;
    }

    public Size getDim() {
        return dim;
    }

    public void setDim(Size dim) {
        this.dim = dim;
    }
}