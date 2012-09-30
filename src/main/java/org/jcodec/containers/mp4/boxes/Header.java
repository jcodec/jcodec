package org.jcodec.containers.mp4.boxes;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jcodec.codecs.wav.StringReader;
import org.jcodec.common.io.ReaderBE;

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

    public void serialize(OutputStream dos) throws IOException {
        long b1 = (size >> 24) & 0xff;
        long b2 = (size >> 16) & 0xff;
        long b3 = (size >> 8) & 0xff;
        long b4 = size & 0xff;

        dos.write((byte) b1);
        dos.write((byte) b2);
        dos.write((byte) b3);
        dos.write((byte) b4);

        dos.write(fourcc.getBytes());
    }

    public static Header read(InputStream input) throws IOException {
        long size = ReaderBE.readInt32(input);
        if (size < 1)
            return null;
        size &= 0xffffffffL;
        String fourcc = ReaderBE.readString(input, 4);
        boolean lng = false;
        if (size == 1) {
            lng = true;
            size = ReaderBE.readInt64(input);
        }

        return new Header(fourcc, size, lng);
    }

    public void print() {
        System.out.println(fourcc + "," + size);
    }

    public void skip(InputStream di) throws IOException {
        StringReader.sureSkip(di, size - headerSize());
    }

    public long headerSize() {
        return lng || (size > 0x100000000L) ? 16 : 8;
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

    public void write(DataOutput out) throws IOException {
        if (size > 0x100000000L)
            out.writeInt(1);
        else
            out.writeInt((int) size);
        out.write(fourcc.getBytes("iso8859-1"));
        if (size > 0x100000000L) {
            out.writeLong(size);
        }
    }

    public long getSize() {
        return size;
    }
}
