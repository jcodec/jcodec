package org.jcodec.codecs.h264.io.write;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class NALUnitWriter {
    private final WritableByteChannel to;
    private static ByteBuffer _MARKER = ByteBuffer.allocate(4);

    static {
        _MARKER.putInt(1);
        _MARKER.flip();
    }

    public NALUnitWriter(WritableByteChannel to) {
        this.to = to;
    }

    public void writeUnit(NALUnit nal, ByteBuffer data) throws IOException {
        ByteBuffer emprev = ByteBuffer.allocate(data.remaining() + 1024);
        NIOUtils.write(emprev, _MARKER);
        nal.write(emprev);
        emprev(emprev, data);
        emprev.flip();
        to.write(emprev);
    }

    private void emprev(ByteBuffer emprev, ByteBuffer data) {
        ByteBuffer dd = data.duplicate();
        int prev1 = 1, prev2 = 1;
        while (dd.hasRemaining()) {
            byte b = dd.get();
            if (prev1 == 0 && prev2 == 0 && ((b & 0x3) == b)) {
                prev2 = prev1;
                prev1 = 3;
                emprev.put((byte) 3);
            }

            prev2 = prev1;
            prev1 = b;
            emprev.put((byte) b);
        }
    }
}
