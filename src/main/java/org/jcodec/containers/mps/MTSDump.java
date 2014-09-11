package org.jcodec.containers.mps;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jcodec.common.Assert;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

public class MTSDump extends MPSDump {
    private static final String DUMP_FROM = "dump-from";
    private static final String STOP_AT = "stop-at";

    private int guid;
    private ByteBuffer buf = ByteBuffer.allocate(188 * 1024);
    private ByteBuffer tsBuf = ByteBuffer.allocate(188);
    private int tsNo;
    private int globalPayload;
    private int[] payloads;
    private int[] nums;
    private int[] prevPayloads;
    private int[] prevNums;

    public MTSDump(ReadableByteChannel ch, int targetGuid) {
        super(ch);
        this.guid = targetGuid;
        this.buf.position(buf.limit());
        this.tsBuf.position(tsBuf.limit());
    }

    public static void main(String[] args) throws IOException {
        ReadableByteChannel ch = null;
        try {
            Cmd cmd = MainUtils.parseArguments(args);
            if (cmd.args.length < 1) {
                MainUtils.printHelp(new HashMap<String, String>() {
                    {
                        put(STOP_AT, "Stop reading at timestamp");
                        put(DUMP_FROM, "Start dumping from timestamp");
                    }
                }, "file name", "guid");
                return;
            } else if (cmd.args.length == 1) {
                System.out.println("MTS programs:");
                dumpProgramPids(NIOUtils.readableFileChannel(new File(cmd.args[0])));
                return;
            }

            ch = NIOUtils.readableFileChannel(new File(cmd.args[0]));
            Long dumpAfterPts = cmd.getLongFlag(DUMP_FROM);
            Long stopPts = cmd.getLongFlag(STOP_AT);

            new MTSDump(ch, Integer.parseInt(cmd.args[1])).dump(dumpAfterPts, stopPts);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    private static void dumpProgramPids(ReadableByteChannel readableFileChannel) throws IOException {
        Set<Integer> pids = new HashSet<Integer>();
        ByteBuffer buf = ByteBuffer.allocate(188 * 1024);
        readableFileChannel.read(buf);
        buf.flip();
        buf.limit(buf.limit() - (buf.limit() % 188));
        while (buf.hasRemaining()) {
            ByteBuffer tsBuf = NIOUtils.read(buf, 188);
            Assert.assertEquals(0x47, tsBuf.get() & 0xff);
            int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
            int guid = (int) guidFlags & 0x1fff;
            if (guid != 0)
                pids.add(guid);
        }
        for (Integer pid : pids) {
            System.out.println(pid);
        }
    }

    protected void logPes(PESPacket pkt, int hdrSize, ByteBuffer payload) {
        System.out.println(pkt.streamId + "(" + (pkt.streamId >= 0xe0 ? "video" : "audio") + ")" + " [ts#"
                + mapPos(pkt.pos) + ", " + (payload.remaining() + hdrSize) + "b], pts: " + pkt.pts + ", dts: "
                + pkt.dts);
    }

    private int mapPos(long pos) {
        int left = globalPayload;
        for (int i = payloads.length - 1; i >= 0; --i) {
            left -= payloads[i];
            if (left <= pos) {
                return nums[i];
            }
        }
        if (prevPayloads != null) {
            for (int i = prevPayloads.length - 1; i >= 0; --i) {
                left -= prevPayloads[i];
                if (left <= pos) {
                    return prevNums[i];
                }
            }
        }
        return -1;
    }

    @Override
    public int fillBuffer(ByteBuffer dst) throws IOException {
        IntArrayList payloads = new IntArrayList();
        IntArrayList nums = new IntArrayList();

        int remaining = dst.remaining();
        dst.put(NIOUtils.read(tsBuf, Math.min(dst.remaining(), tsBuf.remaining())));
        while (dst.hasRemaining()) {
            if (!buf.hasRemaining()) {
                ByteBuffer dub = buf.duplicate();
                dub.clear();
                int read = ch.read(dub);
                if (read == -1)
                    return dst.remaining() != remaining ? remaining - dst.remaining() : -1;
                dub.flip();
                dub.limit(dub.limit() - (dub.limit() % 188));
                buf = dub;
            }

            tsBuf = NIOUtils.read(buf, 188);
            Assert.assertEquals(0x47, tsBuf.get() & 0xff);
            ++tsNo;
            int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
            int guid = (int) guidFlags & 0x1fff;
            if (guid != this.guid)
                continue;
            int payloadStart = (guidFlags >> 14) & 0x1;
            int b0 = tsBuf.get() & 0xff;
            int counter = b0 & 0xf;
            if ((b0 & 0x20) != 0) {
                NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
            }

            globalPayload += tsBuf.remaining();
            payloads.add(tsBuf.remaining());
            nums.add(tsNo - 1);

            dst.put(NIOUtils.read(tsBuf, Math.min(dst.remaining(), tsBuf.remaining())));
        }
        this.prevPayloads = this.payloads;
        this.payloads = payloads.toArray();
        this.prevNums = this.nums;
        this.nums = nums.toArray();
        return remaining - dst.remaining();
    }
}
