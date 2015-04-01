package org.jcodec.codecs.mpeg12;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.MTSUtils;

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
            SeekableByteChannel ch = new FileChannelWrapper(ra.getChannel());
            new MTSUtils.TSReader(true) {
                @Override
                public boolean onPkt(int guid, boolean payloadStart, ByteBuffer bb, long filePos,
                        boolean sectionSyntax, ByteBuffer fullPkt) {
                    return processPacket(payloadStart, bb, sectionSyntax, fullPkt);
                }
            }.readTsFile(ch);
        } finally {
            if (ra != null)
                ra.close();
        }
    }

    private boolean processPacket(boolean payloadStart, ByteBuffer bb, boolean sectionSyntax, ByteBuffer fullPkt) {
        if (!payloadStart || sectionSyntax)
            return true;

        int streamId = bb.getInt();
        if (streamId == 0x1bd || streamId >= 0x1c0 && streamId < 0x1ef) {
            System.out.println("PES: " + streamId);
            int len = bb.getShort();
            int b0 = bb.get() & 0xff;

            bb.position(bb.position() - 1);
            if ((b0 & 0xc0) == 0x80)
                fixMpeg2(streamId & 0xff, bb);
            else
                fixMpeg1(streamId & 0xff, bb);
        }

        return true;
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