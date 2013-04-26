package org.jcodec.codecs.mpeg4.es;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

public class UnknownDescriptor extends Descriptor {

    private byte[] data;

    public UnknownDescriptor(int tag, int size) {
        super(tag, size);
    }

    public UnknownDescriptor(int tag, byte[] data) {
        super(tag, data.length);
        this.data = data;
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.write(data);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    protected void parse(InputStream input) throws IOException {
        data = ReaderBE.readAll(input);
    }
}
