package org.jcodec.containers.mkv;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;

public class MKVShiftTimecodes extends MKVEdit {
    private int offset;

    public MKVShiftTimecodes(int offset) {
        this.offset = offset;
    }

    public static void main(String[] args) throws IOException {
        FileChannelWrapper ch = null;
        FileChannelWrapper out = null;
        try {
            ch = NIOUtils.readableFileChannel(new File(args[0]));
            out = NIOUtils.writableFileChannel(new File(args[1]));
            new MKVShiftTimecodes(Integer.parseInt(args[2])).run(ch, out);
        } finally {
            NIOUtils.closeQuietly(ch);
            NIOUtils.closeQuietly(out);
        }
    }

    private static long readInt(ByteBuffer contents, long size) {
        long ret = 0;
        for (int i = 0; i < size; i++) {
            ret = (ret << 8) | (contents.get() & 0xff);
        }
        return ret;
    }

    @Override
    protected ByteBuffer edit(int type, ByteBuffer contents) {
        if (type == 0xe7) {
            long tc = readInt(contents, contents.remaining());
            System.out.println("TIMECODE: " + tc);
            return (ByteBuffer) ByteBuffer.allocate(4).putInt((int) tc + offset).flip();
        } else
            return contents.duplicate();
    }
}
