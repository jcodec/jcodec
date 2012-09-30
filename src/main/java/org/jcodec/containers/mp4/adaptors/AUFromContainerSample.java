package org.jcodec.containers.mp4.adaptors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.codecs.h264.AccessUnit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An access unit read from a container sample (MP4 like format)
 * 
 * @author Jay Codec
 * 
 */
public class AUFromContainerSample implements AccessUnit {
    private byte[] buffer;
    private int pos;

    public AUFromContainerSample(byte[] payload) {
        this.buffer = payload;
    }

    public InputStream nextNALUnit() throws IOException {
        if (pos + 4 >= buffer.length)
            return null;

        int len = ((buffer[pos] & 0xff) << 24) + ((buffer[pos + 1] & 0xff) << 16) + ((buffer[pos + 2] & 0xff) << 8)
                + (buffer[pos + 3] & 0xff);
        ByteArrayInputStream result = new ByteArrayInputStream(buffer, pos + 4, len);
        pos += len + 4;
        return result;
    }
}