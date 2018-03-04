package org.jcodec.containers.mps;

import static java.util.Arrays.asList;
import static org.jcodec.common.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.IntIntMap;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.mps.MPSUtils.MPEGMediaDescriptor;
import org.jcodec.containers.mps.psi.PATSection;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MTSDump extends MPSDump {
    private static final Flag DUMP_FROM = Flag.flag("dump-from", null, "Stop reading at timestamp");
    private static final Flag STOP_AT = Flag.flag("stop-at", null, "Start dumping from timestamp");
    private static final Flag[] ALL_FLAGS = new Flag[] { DUMP_FROM, STOP_AT };

    private int guid;
    private ByteBuffer buf;
    private ByteBuffer tsBuf;
    private int tsNo;
    private int globalPayload;
    private int[] payloads;
    private int[] nums;
    private int[] prevPayloads;
    private int[] prevNums;

    public MTSDump(ReadableByteChannel ch, int targetGuid) {
        super(ch);
        this.buf = ByteBuffer.allocate(188 * 1024);
        this.tsBuf = ByteBuffer.allocate(188);

        this.guid = targetGuid;
        this.buf.position(buf.limit());
        this.tsBuf.position(tsBuf.limit());
    }

    public static void main2(String[] args) throws IOException {
        ReadableByteChannel ch = null;
        try {
            Cmd cmd = MainUtils.parseArguments(args, ALL_FLAGS);
            if (cmd.args.length < 1) {
                MainUtils.printHelp(ALL_FLAGS, asList("file name", "guid"));
                return;
            } else if (cmd.args.length == 1) {
                System.out.println("MTS programs:");
                dumpProgramPids(NIOUtils.readableChannel(new File(cmd.args[0])));
                return;
            }

            ch = NIOUtils.readableChannel(new File(cmd.args[0]));
            Long dumpAfterPts = cmd.getLongFlag(DUMP_FROM);
            Long stopPts = cmd.getLongFlag(STOP_AT);

            new MTSDump(ch, Integer.parseInt(cmd.args[1])).dump(dumpAfterPts, stopPts);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    private static void dumpProgramPids(ReadableByteChannel readableFileChannel) throws IOException {
        Set<Integer> pids = new HashSet<Integer>();
        ByteBuffer buf = ByteBuffer.allocate(188 * 10240);
        readableFileChannel.read(buf);
        buf.flip();
        buf.limit(buf.limit() - (buf.limit() % 188));
        int pmtPid = -1;
        while (buf.hasRemaining()) {
            ByteBuffer tsBuf = NIOUtils.read(buf, 188);
            checkState(0x47 == (tsBuf.get() & 0xff));
            int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
            int guid = guidFlags & 0x1fff;
            System.out.println(guid);
            if (guid != 0)
                pids.add(guid);
            if (guid == 0 || guid == pmtPid) {
                // PSI
                int payloadStart = (guidFlags >> 14) & 0x1;
                int b0 = tsBuf.get() & 0xff;
                int counter = b0 & 0xf;
                int payloadOff = 0;
                if ((b0 & 0x20) != 0) {
                    NIOUtils.skip(tsBuf, (tsBuf.get() & 0xff));
                }
                if (payloadStart == 1) {
                    NIOUtils.skip(tsBuf, (tsBuf.get() & 0xff));
                }

                if (guid == 0) {
                    PATSection pat = PATSection.parsePAT(tsBuf);
                    IntIntMap programs = pat.getPrograms();
                    pmtPid = programs.values()[0];
                    printPat(pat);
                } else if (guid == pmtPid) {
                    PMTSection pmt = PMTSection.parsePMT(tsBuf);
                    printPmt(pmt);
                    return;
                }
            }
        }
        for (Integer pid : pids) {
            System.out.println(pid);
        }
    }

    private static void printPat(PATSection pat) {
        IntIntMap programs = pat.getPrograms();
        System.out.print("PAT: ");
        int[] keys = programs.keys();
        for (int i : keys) {
            System.out.print(i + ":" + programs.get(i) + ", ");
        }
        System.out.println();
    }

    private static void printPmt(PMTSection pmt) {
        System.out.print("PMT: ");
        for (PMTStream pmtStream : pmt.getStreams()) {
            System.out.print(pmtStream.getPid() + ":" + pmtStream.getStreamTypeTag() + ", ");
            for (MPEGMediaDescriptor descriptor : pmtStream.getDesctiptors()) {
                System.out.println(ToJSON.toJSON(descriptor));
            }
        }
        System.out.println();
    }

    protected void logPes(PESPacket pkt, int hdrSize, ByteBuffer payload) {
        System.out.println(
                pkt.streamId + "(" + (pkt.streamId >= 0xe0 ? "video" : "audio") + ")" + " [ts#" + mapPos(pkt.pos) + ", "
                        + (payload.remaining() + hdrSize) + "b], pts: " + pkt.pts + ", dts: " + pkt.dts);
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
        IntArrayList payloads = IntArrayList.createIntArrayList();
        IntArrayList nums = IntArrayList.createIntArrayList();
        int remaining = dst.remaining();

        try {
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
                checkState(0x47 == (tsBuf.get() & 0xff));
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
        } finally {
            this.prevPayloads = this.payloads;
            this.payloads = payloads.toArray();
            this.prevNums = this.nums;
            this.nums = nums.toArray();
        }
        return remaining - dst.remaining();
    }
}
