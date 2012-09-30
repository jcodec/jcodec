package org.jcodec.codecs.h264.io.model;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public abstract class BitstreamElement {

    public abstract void write(OutputStream out) throws IOException;
}
