package org.jcodec.codecs.vpx;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IVFMuxer {

    private SeekableByteChannel ch;
    private int nFrames;

    public IVFMuxer(SeekableByteChannel ch, int w, int h, int frameRate) throws IOException {
        ByteBuffer ivf = ByteBuffer.allocate(32);
        ivf.order(ByteOrder.LITTLE_ENDIAN);

        ivf.put((byte) 'D');
        ivf.put((byte) 'K');
        ivf.put((byte) 'I');
        ivf.put((byte) 'F');
        ivf.putShort((short) 0);/* version */
        ivf.putShort((short) 32); /* headersize */
        ivf.putInt(0x30385056); /* headersize */
        ivf.putShort((short) w); /* width */
        ivf.putShort((short) h); /* height */
        ivf.putInt(frameRate); /* rate */
        ivf.putInt(1); /* scale */
        ivf.putInt(1); /* length */
        ivf.clear();

        ch.write(ivf);

        this.ch = ch;
    }

    public void addFrame(Packet pkt) throws IOException {

        ByteBuffer fh = ByteBuffer.allocate(12);
        fh.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer frame = pkt.getData();
        fh.putInt(frame.remaining());
        fh.putLong(nFrames);
        fh.clear();

        ch.write(fh);
        ch.write(frame);

        nFrames++;
    }

    public void close() throws IOException {
        ch.setPosition(24);
        NIOUtils.writeIntLE(ch, nFrames);
    }
}