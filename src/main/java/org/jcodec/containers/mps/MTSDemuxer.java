package org.jcodec.containers.mps;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.ReaderBE;
import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG TS demuxer
 * 
 * @author The JCodec project
 * 
 */
public class MTSDemuxer extends MPSDemuxer {
    private InputStream is;
    private int guid = -1;

    public MTSDemuxer(File f) throws IOException {
        is = new BufferedInputStream(new FileInputStream(f));
        findStreams();
    }

    protected MTSDemuxer() throws IOException {
    }

    @Override
    protected Buffer fetchBuffer() throws IOException {
        MTSPacket packet = getPacket(is);
        return packet != null ? packet.payload : null;
    }

    protected MTSPacket getPacket(InputStream iis) throws IOException {
        MTSPacket pkt;
        do {
            pkt = readPacket(iis);
            if (pkt == null)
                return null;
        } while (pkt.pid <= 0xf || pkt.pid == 0x1fff);

        while (guid == -1) {
            Buffer payload = pkt.payload;
            if (payload.get(0) == 0 && payload.get(1) == 0 && payload.get(2) == 1) {
                guid = pkt.pid;
                break;
            }
            pkt = readPacket(iis);
            if (pkt == null)
                return null;
        }

        while (pkt.pid != guid) {
            pkt = readPacket(iis);
            if (pkt == null)
                return null;
        }

        // pkt.payload.print(System.out);
        // System.out.println(",");

        return pkt;
    }

    public static class MTSPacket {
        public Buffer payload;
        public boolean payloadStart;
        public int pid;

        public MTSPacket(int guid, boolean payloadStart, Buffer payload) {
            this.pid = guid;
            this.payloadStart = payloadStart;
            this.payload = payload;
        }
    }

    public static MTSPacket readPacket(InputStream iis) throws IOException {
        int marker = iis.read();
        if (marker == -1)
            return null;
        Assert.assertEquals(0x47, marker);
        int guidFlags = (int) ReaderBE.readInt16(iis);
        int guid = (int) guidFlags & 0x1fff;
        int payloadStart = (guidFlags >> 14) & 0x1;
        int b0 = iis.read();
        int counter = b0 & 0xf;
        int taken = 0;
        if ((b0 & 0x20) != 0) {
            taken = iis.read() + 1;
            ReaderBE.sureRead(iis, taken - 1);
        }
        Buffer payload = null;
        if ((b0 & 0x10) != 0) {
            payload = Buffer.fetchFrom(iis, 184 - taken);
        }
        return new MTSPacket(guid, payloadStart == 1, payload);
    }

    public static int probe(final Buffer b) {
        InputStream iis = b.is();
        TIntObjectHashMap<List<Buffer>> streams = new TIntObjectHashMap<List<Buffer>>();
        while (true) {
            try {
                MTSPacket tsPkt = readPacket(iis);
                if (tsPkt == null)
                    break;
                List<Buffer> data = streams.get(tsPkt.pid);
                if (data == null) {
                    data = new ArrayList<Buffer>();
                    streams.put(tsPkt.pid, data);
                }
                data.add(tsPkt.payload);
            } catch (Throwable t) {
                break;
            }
        }
        int maxScore = 0;
        int[] keys = streams.keys();
        for (int i : keys) {
            int score = MPSDemuxer.probe(Buffer.combine(streams.get(i)));
            if (score > maxScore)
                maxScore = score;
        }
        return maxScore;
    }
}