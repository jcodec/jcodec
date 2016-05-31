package org.jcodec.containers.mkv.boxes;
import org.jcodec.common.io.SeekableByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EbmlVoid extends EbmlBase {

    public EbmlVoid(byte[] id) {
        super(id);
    }

    @Override
    public ByteBuffer getData() {
        return null;
    }
    
    public void skip(SeekableByteChannel is) throws IOException{
        is.setPosition(this.dataOffset+this.dataLen);
    }

}
