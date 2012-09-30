package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An interface for retrieving stream parameters from a place that has it
 * 
 * @author Jay Codec
 * 
 */
public interface StreamParams {
    SeqParameterSet getSPS(int id);

    PictureParameterSet getPPS(int id);
}
