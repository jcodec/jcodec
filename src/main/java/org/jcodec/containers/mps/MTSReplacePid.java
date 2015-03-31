package org.jcodec.containers.mps;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jcodec.common.IntIntMap;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MTSUtils.StreamType;
import org.jcodec.containers.mps.psi.PATSection;
import org.jcodec.containers.mps.psi.PSISection;

/**
 * Replaces pid of a stream with a different value
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class MTSReplacePid implements MTSUtils.TSFilter {

    private Set<Integer> pmtPids = new HashSet<Integer>();
    private IntIntMap replaceSpec;

    public MTSReplacePid(IntIntMap replaceSpec) {
        this.replaceSpec = replaceSpec;
    }

    @Override
    public boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos, boolean sectionSyntax,
            ByteBuffer fullPkt) {
        if (sectionSyntax) {
            replaceRefs(replaceSpec, guid, tsBuf, pmtPids);
        } else {
            System.out.print("TS ");
            ByteBuffer buf = fullPkt.duplicate();
            short tsFlags = buf.getShort(buf.position() + 1);
            buf.putShort(buf.position() + 1, (short) (replacePid(replaceSpec, tsFlags & 0x1fff) | (tsFlags & ~0x1fff)));
        }
        return true;
    }

    private static IntIntMap parseReplaceSpec(String spec) {
        IntIntMap map = new IntIntMap();
        for (String pidPair : spec.split(",")) {
            String[] pidPairParsed = pidPair.split(":");
            map.put(Integer.parseInt(pidPairParsed[0]), Integer.parseInt(pidPairParsed[1]));

        }
        return map;
    }

    private void replaceRefs(IntIntMap replaceSpec, int guid, ByteBuffer buf, Set<Integer> pmtPids) {
        if (guid == 0) {
            PATSection pat = PATSection.parse(buf);
            for (int pids : pat.getPrograms().values()) {
                pmtPids.add(pids);
            }
        } else if (pmtPids.contains(guid)) {
            System.out.println(MainUtils.bold("PMT"));
            PSISection.parse(buf);

            buf.getShort();

            NIOUtils.skip(buf, buf.getShort() & 0xfff);

            while (buf.remaining() > 4) {
                byte streamType = buf.get();
                StreamType fromTag = MTSUtils.StreamType.fromTag(streamType);
                System.out.print((fromTag == null ? "UNKNOWN" : fromTag) + "(" + String.format("0x%02x", streamType)
                        + "):\t");
                int wn = buf.getShort() & 0xffff;
                int wasPid = wn & 0x1fff;
                int elementaryPid = replacePid(replaceSpec, wasPid);
                buf.putShort(buf.position() - 2, (short) ((elementaryPid & 0x1fff) | (wn & ~0x1fff)));

                NIOUtils.skip(buf, buf.getShort() & 0xfff);
            }
        }
    }

    private int replacePid(IntIntMap replaceSpec, int pid) {
        int newPid = pid;
        if (replaceSpec.contains(pid)) {
            newPid = replaceSpec.get(pid);
        }
        System.out.println("[" + pid + "->" + newPid + "]");
        return newPid;
    }

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.args.length < 3) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                }
            }, "pid_from:pid_to,[pid_from:pid_to...]", "in file", "out file");
            return;
        }

        IntIntMap replaceSpec = parseReplaceSpec(cmd.getArg(0));

        SeekableByteChannel in = null;
        SeekableByteChannel out = null;
        try {
            in = NIOUtils.readableFileChannel(new File(cmd.getArg(1)));
            out = NIOUtils.writableFileChannel(new File(cmd.getArg(2)));

            MTSReplacePid replacer = new MTSReplacePid(replaceSpec);

            new MTSUtils.TSReader(new Sink(replacer, out)).readTsFile(in);
        } finally {
            NIOUtils.closeQuietly(in);
            NIOUtils.closeQuietly(out);
        }
    }

    private static class Sink implements MTSUtils.TSFilter {

        private MTSUtils.TSFilter chain;
        private SeekableByteChannel out;

        public Sink(MTSUtils.TSFilter chain, SeekableByteChannel out) {
            this.chain = chain;
            this.out = out;
        }

        @Override
        public boolean onPkt(int guid, boolean payloadStart, ByteBuffer tsBuf, long filePos, boolean sectionSyntax,
                ByteBuffer fullPkt) {
            if (!chain.onPkt(guid, payloadStart, tsBuf, filePos, sectionSyntax, fullPkt))
                return false;

            try {
                out.write(fullPkt);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }
}
