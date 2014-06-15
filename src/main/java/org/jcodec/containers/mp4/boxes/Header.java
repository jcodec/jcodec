package org.jcodec.containers.mp4.boxes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jcodec.codecs.wav.StringReader;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.logging.Logger;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An MP4 file structure (atom)
 * 
 * @author Jay Codec
 * 
 */
public class Header {

    private static final long MAX_UNSIGNED_INT = 0x100000000L;
    private String fourcc;
    private long size;
    private boolean lng;

    public Header(String fourcc) {
        this.fourcc = fourcc;
    }

    public Header(String fourcc, long size) {
        this.size = size;
        this.fourcc = fourcc;
    }

    public Header(Header h) {
        this.fourcc = h.fourcc;
        this.size = h.size;
    }

    public Header(String fourcc, long size, boolean lng) {
        this(fourcc, size);
        this.lng = lng;
    }

    public static Header read(ByteBuffer input) {
        long size = 0;
        while (input.remaining() >= 4 && (size = (((long) input.getInt()) & 0xffffffffL)) == 0)
            ;
        if (input.remaining() < 4 || size < 8 && size != 1) {
            Logger.error("Broken atom of size " + size);
            return null;
        }

        String fourcc = NIOUtils.readString(input, 4);
        boolean lng = false;
        if (size == 1) {
            if (input.remaining() >= 8) {
                lng = true;
                size = input.getLong();
            } else {
                Logger.error("Broken atom of size " + size);
                return null;
            }
        }

        return new Header(fourcc, size, lng);
    }

    public void skip(InputStream di) throws IOException {
        StringReader.sureSkip(di, size - headerSize());
    }

    public long headerSize() {
        return lng || (size > MAX_UNSIGNED_INT) ? 16 : 8;
    }

    public byte[] readContents(InputStream di) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < size - headerSize(); i++) {
            baos.write(di.read());
        }
        return baos.toByteArray();
    }

    public String getFourcc() {
        return fourcc;
    }

    public long getBodySize() {
        return size - headerSize();
    }

    public void setBodySize(int length) {
        size = length + headerSize();
    }

    public void write(ByteBuffer out) {
        if (size > MAX_UNSIGNED_INT)
            out.putInt(1);
        else
            out.putInt((int) size);
        out.put(JCodecUtil.asciiString(fourcc));
        if (size > MAX_UNSIGNED_INT) {
            out.putLong(size);
        }
    }
    
    public void write(SeekableByteChannel output) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(16);
        write(bb);
        bb.flip();
        output.write(bb);
    }

    public long getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fourcc == null) ? 0 : fourcc.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Header other = (Header) obj;
        if (fourcc == null) {
            if (other.fourcc != null)
                return false;
        } else if (!fourcc.equals(other.fourcc))
            return false;
        return true;
    }
}