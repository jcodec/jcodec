package org.jcodec.codecs.h264.io.read;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.io.model.NALUnit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public interface ReadableTransportUnit {

    NALUnit getNu();

    ByteBuffer getContents();

    InputStream getInputStream();
}