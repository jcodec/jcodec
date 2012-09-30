package org.jcodec.codecs.h264.io.write;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.io.model.NALUnit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public interface WritableTransportUnit {
    NALUnit getNu();

    void getContents(ByteBuffer newContents);

    OutputStream getOutputStream();

}
