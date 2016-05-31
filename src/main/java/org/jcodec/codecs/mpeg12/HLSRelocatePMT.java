package org.jcodec.codecs.mpeg12;
import org.jcodec.common.Assert;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.psi.PATSection;

import java.io.File;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This utility relocates PAT/PMT PSI packets from anywhere within a file to the
 * beginning of the file so that the file is playable
 * 
 * @author The JCodec project
 * 
 */
public class HLSRelocatePMT {
    private static final int TS_START_CODE = 0x47;
    private static final int CHUNK_SIZE_PKT = 1024;
    private static final int TS_PKT_SIZE = 188;

    public static void main1(String[] args) throws IOException {

        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.args.length < 2) {
            MainUtils.printHelpNoFlags("file _in", "file out");
            return;
        }

        ReadableByteChannel _in = null;
        WritableByteChannel out = null;
        try {
            _in = NIOUtils.readableChannel(new File(cmd.args[0]));
            out = NIOUtils.writableChannel(new File(cmd.args[1]));
            System.err.println("Processed: " + replocatePMT(_in, out) + " packets.");
        } finally {
            NIOUtils.closeQuietly(_in);
            NIOUtils.closeQuietly(out);
        }
    }

    private static int replocatePMT(ReadableByteChannel _in, WritableByteChannel out) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(TS_PKT_SIZE * CHUNK_SIZE_PKT);
        Set<Integer> pmtPids = new HashSet<Integer>();
        List<ByteBuffer> held = new ArrayList<ByteBuffer>();
        ByteBuffer patPkt = null;
        ByteBuffer pmtPkt = null;

        int totalPkt = 0;
        while (_in.read(buf) != -1) {
            buf.flip();
            buf.limit((buf.limit() / TS_PKT_SIZE) * TS_PKT_SIZE);

            while (buf.hasRemaining()) {
                ByteBuffer pkt = NIOUtils.read(buf, TS_PKT_SIZE);
                ByteBuffer pktRead = pkt.duplicate();
                Assert.assertEquals(TS_START_CODE, pktRead.get() & 0xff);
                ++totalPkt;
                int guidFlags = ((pktRead.get() & 0xff) << 8) | (pktRead.get() & 0xff);
                int guid = (int) guidFlags & 0x1fff;
                int payloadStart = (guidFlags >> 14) & 0x1;
                int b0 = pktRead.get() & 0xff;
                int counter = b0 & 0xf;
                if ((b0 & 0x20) != 0) {
                    NIOUtils.skip(pktRead, (pktRead.get() & 0xff));
                }

                if (guid == 0 || pmtPids.contains(guid)) {
                    if (payloadStart == 1) {
                        NIOUtils.skip(pktRead, (pktRead.get() & 0xff));
                    }

                    if (guid == 0) {
                        patPkt = pkt;
                        PATSection pat = PATSection.parsePAT(pktRead);
                        int[] values = pat.getPrograms().values();
                        for (int i = 0; i < values.length; i++) {
                            int pmtPid = values[i];
                            pmtPids.add(pmtPid);
                        }
                    } else if (pmtPids.contains(guid)) {
                        pmtPkt = pkt;
                        out.write(patPkt);
                        out.write(pmtPkt);
                        for (ByteBuffer heldPkt : held) {
                            out.write(heldPkt);
                        }
                        held.clear();
                    }
                } else {
                    if (pmtPkt == null)
                        held.add(pkt);
                    else
                        out.write(pkt);
                }
            }
            buf.clear();
        }
        return totalPkt;
    }
}