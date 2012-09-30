package org.jcodec.codecs.h264;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public interface AccessUnit {
    InputStream nextNALUnit() throws IOException;
}
