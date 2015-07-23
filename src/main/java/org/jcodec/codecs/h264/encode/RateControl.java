package org.jcodec.codecs.h264.encode;

import org.jcodec.codecs.h264.io.model.SliceType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 4 AVC ( H.264 ) Encoder pluggable rate control mechanism
 * 
 * @author The JCodec project
 * 
 */
public interface RateControl {

    int getInitQp(SliceType sliceType);

    int getQpDelta();

    boolean accept(int bits);

    void reset();

}
