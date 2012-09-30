package org.jcodec.codecs.mpeg4.es;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DecoderSpecific extends Descriptor {

    private byte[] data;

    public DecoderSpecific(int tag, int size) {
        super(tag, size);
    }
    
    public DecoderSpecific(byte[] data) {
        super(tag());
        this.data = data;
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(data);
    }

    public static int tag() {
        return 0x5;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    protected void parse(InputStream input) throws IOException {
        data = ReaderBE.readAll(input);
    }
}
