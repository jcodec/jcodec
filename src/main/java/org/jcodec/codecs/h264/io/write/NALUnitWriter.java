package org.jcodec.codecs.h264.io.write;

import java.io.IOException;
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
public class NALUnitWriter implements ElementaryStreamWriter {
    private final OutputStream to;
    private int prev1;
    private int prev2;

    public NALUnitWriter(OutputStream to) {
        this.to = to;
        prev1 = -1;
        prev2 = -1;
    }

    private class ProxyOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            writeByte(b);
        }

    }

    /**
     * Outputs a NAL unit marker
     * 
     * @throws IOException
     */
    public void writeMarker() throws IOException {
        to.write(0);
        to.write(0);
        to.write(0);
        to.write(1);
    }

    public void writeByte(int b) throws IOException {
        if (prev1 == 0 && prev2 == 0 && ((b & 0x3) == b)) {
            prev2 = prev1;
            prev1 = 3;
            to.write(3);
        }

        prev2 = prev1;
        prev1 = b;
        to.write(b);
    }

    public WritableTransportUnit writeUnit(final NALUnit nal) throws IOException {
        final OutputStream out = new ProxyOutputStream();

        WritableTransportUnit unit = new WritableTransportUnit() {

            public void getContents(ByteBuffer newContents) {
                throw new UnsupportedOperationException("Unsupported");
            }

            public NALUnit getNu() {
                return nal;
            }

            public OutputStream getOutputStream() {
                return out;
            }
        };

        writeMarker();
        nal.write(to);

        return unit;
    }
}
