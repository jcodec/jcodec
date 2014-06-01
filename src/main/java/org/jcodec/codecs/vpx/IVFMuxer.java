package org.jcodec.codecs.vpx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import org.jcodec.common.NIOUtils;

public class IVFMuxer {
    public static void writeIVF(File out, ByteBuffer frame, int width, int height, int frames, int rate)
            throws IOException {
        ByteBuffer ivf = ByteBuffer.allocate(32);
        ivf.order(ByteOrder.LITTLE_ENDIAN);

        ivf.put((byte) 'D');
        ivf.put((byte) 'K');
        ivf.put((byte) 'I');
        ivf.put((byte) 'F');
        ivf.putShort((short) 0);/* version */
        ivf.putShort((short) 32); /* headersize */
        ivf.putInt(0x30385056); /* headersize */
        ivf.putShort((short) width); /* width */
        ivf.putShort((short) height); /* height */
        ivf.putInt(rate); /* rate */
        ivf.putInt(1); /* scale */
        ivf.putInt(frames); /* length */
        ivf.clear();

        ByteBuffer fh = ByteBuffer.allocate(12);
        fh.order(ByteOrder.LITTLE_ENDIAN);
        fh.putInt(frame.remaining());
        fh.putLong(0);
        fh.clear();

        FileChannel ch = new FileOutputStream(out).getChannel();
        ch.write(ivf);
        ch.write(fh);
        ch.write(frame);
        NIOUtils.closeQuietly(ch);
    }

    public static void writeIVFFrame(WritableByteChannel out, ByteBuffer frame, long pts) throws IOException {
        ByteBuffer fh = ByteBuffer.allocate(12);
        fh.order(ByteOrder.LITTLE_ENDIAN);
        fh.putInt(frame.remaining());
        fh.putLong(pts);
        fh.clear();

        out.write(fh);
        out.write(frame);
    }

    public static ByteBuffer writeIVFHeader(WritableByteChannel out, int width, int height, int frames, int rate) throws IOException {
        ByteBuffer ivf = ByteBuffer.allocate(32);
        ivf.order(ByteOrder.LITTLE_ENDIAN);

        ivf.put((byte) 'D');
        ivf.put((byte) 'K');
        ivf.put((byte) 'I');
        ivf.put((byte) 'F');
        ivf.putShort((short) 0);/* version */
        ivf.putShort((short) 32); /* headersize */
        ivf.putInt(0x30385056); /* headersize */
        ivf.putShort((short) width); /* width */
        ivf.putShort((short) height); /* height */
        ivf.putInt(rate); /* rate */
        ivf.putInt(1); /* scale */
        ivf.putInt(frames); /* length */
        ivf.clear();
        out.write(ivf);
        return ivf;
    }
}
