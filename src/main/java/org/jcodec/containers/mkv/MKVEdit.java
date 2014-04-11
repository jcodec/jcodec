package org.jcodec.containers.mkv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class MKVEdit {
    public static final int[] MASTER_ELEMENTS = new int[] { 0x1A45DFA3, 0x1B538667, 0x7E5B, 0x7E7B, 0x18538067,
            0x114D9B74, 0x4DBB, 0x1549A966, 0x6924, 0x1F43B675, 0x5854, 0xA0, 0x75A1, 0xA6, 0x8E, 0xE8, 0x1654AE6B,
            0xAE, 0x6624, 0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE9, 0x6D80, 0x6240, 0x5034, 0x5035, 0x1C53BB6B, 0xBB, 0xB7,
            0xDB, 0x1941A469, 0x61A7, 0x1043A770, 0x45B9, 0xB6, 0x8F, 0x80, 0x6944, 0x6911, 0x1254C367, 0x7373, 0x63C0,
            0x67C8 };

    public static final int[] TOP_ELEMENTS = new int[] { 0x1A45DFA3, 0x18538067 };

    protected void run(FileChannelWrapper ch, FileChannelWrapper out) throws IOException {
        ByteBuffer inBuf = NIOUtils.fetchFrom(ch, 65536);

        List<ByteBuffer> list = new ArrayList<ByteBuffer>();
        IntArrayList tags = new IntArrayList();
        long tagPos = 0;
        while (inBuf.hasRemaining()) {
            int tagLen = inBuf.position();
            int type = readVIntNoMask(inBuf);
            if (!ofAClass(TOP_ELEMENTS, type))
                break;
            long size = readVInt(inBuf);
            tagLen = inBuf.position() - tagLen + (int) size;
            if (inBuf.remaining() < size) {
                ch.position(tagPos);
                inBuf = NIOUtils.fetchFrom(ch, (int) (size + 16));
                continue;
            }
            System.out.println(type + ": " + size);
            tags.add(type);
            list.add(processLevel(NIOUtils.read(inBuf, (int) size), "+"));
            tagPos += tagLen;
        }

        for (int i = 0; i < tags.size(); i++) {
            ByteBuffer tag = ByteBuffer.allocate(16);
            writeVIntNoMask(tag, tags.get(i));
            ByteBuffer bb = list.get(i);
            writeVInt(tag, bb.remaining());
            tag.flip();
            out.write(tag);
            out.write(bb.duplicate());
        }

        // Copy the rest
        ch.position(tagPos);
        NIOUtils.copy(ch, out, ch.size() - ch.position());
    }

    private static boolean ofAClass(int[] set, int type) {
        for (int i = 0; i < set.length; i++)
            if (set[i] == type)
                return true;
        return false;
    }

    private ByteBuffer processLevel(ByteBuffer read, String level) {
        List<ByteBuffer> list = new ArrayList<ByteBuffer>();
        IntArrayList tags = new IntArrayList();
        int outSize = 0;
        while (read.hasRemaining()) {
            int type = readVIntNoMask(read);
            long size = readVInt(read);
            System.out.println(type + ": " + size);
            ByteBuffer contents = NIOUtils.read(read, (int) size);
            ByteBuffer outBuf;
            if (ofAClass(MASTER_ELEMENTS, type)) {
                outBuf = processLevel(contents, level + "+");
            } else {
                outBuf = edit(type, contents);
            }
            list.add(outBuf);
            tags.add(type);
            outSize += 16 + outBuf.remaining();
        }
        ByteBuffer out = ByteBuffer.allocate(outSize);
        for (int i = 0; i < tags.size(); i++) {
            writeVIntNoMask(out, tags.get(i));
            ByteBuffer bb = list.get(i);
            writeVInt(out, bb.remaining());
            out.put(bb);
        }
        out.flip();
        return out;
    }

    protected abstract ByteBuffer edit(int type, ByteBuffer contents);

    /**
     * Reads EBML variable length integer
     * 
     * @param bb
     * @return
     */
    public static int readVIntNoMask(ByteBuffer bb) {
        int ret = bb.get() & 0xff;
        int len = 7 - MathUtil.log2(ret);
        if (len > 3)
            throw new RuntimeException("Max 4 bytes expected");
        for (int i = 0; i < len; i++) {
            ret = (ret << 8) | (bb.get() & 0xff);
        }

        return ret;
    }

    /**
     * Reads EBML variable length integer 64 bit
     * 
     * @param bb
     * @return
     */
    public static long readVInt(ByteBuffer bb) {
        long ret = bb.get() & 0xff;
        int len = 7 - MathUtil.log2(ret);
        ret &= (1 << (7 - len)) - 1;
        for (int i = 0; i < len; i++) {
            ret = (ret << 8) | (bb.get() & 0xff);
        }

        return ret;
    }

    public static void writeVInt(ByteBuffer bb, long num) {
        int log = MathUtil.log2(num);
        if (log >= 56)
            throw new RuntimeException("Can not encode more then 56 bits");
        int bytes = log / 7;
        log = bytes << 3;
        int mask = 0x80 >>> bytes;
        for (; log >= 0; log -= 8, mask = 0) {
            bb.put((byte) (mask | (num >>> log)));
        }
    }

    public static void writeVIntNoMask(ByteBuffer bb, int num) {
        int log = MathUtil.log2(num);
        log &= ~7;
        for (; log >= 0; log -= 8) {
            bb.put((byte) (num >>> log));
        }
    }
}