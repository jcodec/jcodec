package org.jcodec.codecs.mpeg12;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.jcodec.common.Assert;
import org.jcodec.common.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class FixTimestamp {
    public void fix(File file) throws IOException {
        RandomAccessFile ra = null;
        try {
            ra = new RandomAccessFile(file, "rw");
            byte[] tsPkt = new byte[188];

            while (ra.read(tsPkt) == 188) {

                Assert.assertEquals(0x47, tsPkt[0] & 0xff);
                int guidFlags = ((tsPkt[1] & 0xff) << 8) | (tsPkt[2] & 0xff);
                int guid = (int) guidFlags & 0x1fff;
                int payloadStart = (guidFlags >> 14) & 0x1;
                if (payloadStart == 0 || guid == 0)
                    continue;
                ByteBuffer bb = ByteBuffer.wrap(tsPkt, 4, 184);
                if ((tsPkt[3] & 0x20) != 0) {
                    NIOUtils.skip(bb, bb.get() & 0xff);
                }
                
                if(bb.remaining() < 10)
                    continue; // non PES payload

                int streamId = bb.getInt();
                if((streamId >> 8) != 1)
                    continue; // non PES payload, probably PSI
                while (bb.hasRemaining() && !(streamId >= 0x1bf && streamId < 0x1ef)) {
                    streamId <<= 8;
                    streamId |= bb.get() & 0xff;
                }
                if (streamId >= 0x1bf && streamId < 0x1ef) {
                    int len = bb.getShort();
                    int b0 = bb.get() & 0xff;

                    bb.position(bb.position() - 1);
                    if ((b0 & 0xc0) == 0x80)
                        fixMpeg2(streamId & 0xff, bb);
                    else
                        fixMpeg1(streamId & 0xff, bb);

                    ra.seek(ra.getFilePointer() - 188);
                    ra.write(tsPkt);
                }
            }
        } finally {
            if (ra != null)
                ra.close();
        }
    }

    public void fixMpeg1(int streamId, ByteBuffer is) {
        int c = is.getInt() & 0xff;
        while (c == 0xff) {
            c = is.get() & 0xff;
        }

        if ((c & 0xc0) == 0x40) {
            is.get();
            c = is.get() & 0xff;
        }
        if ((c & 0xf0) == 0x20) {
            is.position(is.position() - 1);
            fixTs(streamId, is, true);
        } else if ((c & 0xf0) == 0x30) {
            is.position(is.position() - 1);
            fixTs(streamId, is, true);
            fixTs(streamId, is, false);
        } else {
            if (c != 0x0f)
                throw new RuntimeException("Invalid data");
        }
    }

    public long fixTs(int streamId, ByteBuffer is, boolean isPts) {
        byte b0 = is.get();
        byte b1 = is.get();
        byte b2 = is.get();
        byte b3 = is.get();
        byte b4 = is.get();

        long pts = (((long) b0 & 0x0e) << 29) | ((b1 & 0xff) << 22) | (((b2 & 0xff) >> 1) << 15) | ((b3 & 0xff) << 7)
                | ((b4 & 0xff) >> 1);

        pts = doWithTimestamp(streamId, pts, isPts);

        is.position(is.position() - 5);

        is.put((byte) ((b0 & 0xf0) | (pts >>> 29) | 1));
        is.put((byte) (pts >>> 22));
        is.put((byte) ((pts >>> 14) | 1));
        is.put((byte) (pts >>> 7));
        is.put((byte) ((pts << 1) | 1));

        return pts;
    }

    public void fixMpeg2(int streamId, ByteBuffer is) {
        int flags1 = is.get() & 0xff;
        int flags2 = is.get() & 0xff;
        int header_len = is.get() & 0xff;

        if ((flags2 & 0xc0) == 0x80) {
            fixTs(streamId, is, true);
        } else if ((flags2 & 0xc0) == 0xc0) {
            fixTs(streamId, is, true);
            fixTs(streamId, is, false);
        }
    }

    public boolean isVideo(int streamId) {
        return streamId >= 0xe0 && streamId <= 0xef;
    }

    public boolean isAudio(int streamId) {
        return streamId >= 0xbf && streamId <= 0xdf;
    }

    protected abstract long doWithTimestamp(int streamId, long pts, boolean isPts);
}