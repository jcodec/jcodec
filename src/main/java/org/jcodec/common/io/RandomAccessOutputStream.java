package org.jcodec.common.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class RandomAccessOutputStream extends OutputStream implements DataOutput {
    
    public abstract long getPos();
    
    public abstract void seek(long pos) throws IOException;

}
