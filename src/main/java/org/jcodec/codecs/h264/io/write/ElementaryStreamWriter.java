package org.jcodec.codecs.h264.io.write;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.NALUnit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public interface ElementaryStreamWriter {

    WritableTransportUnit writeUnit(NALUnit nu) throws IOException;
}
