package org.jcodec.codecs.h264.io.read.rtp;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents one RTP packet
 * 
 * @author Jay Codec
 * 
 */
public class Packet {
    public static class Header {
        private int v;
        private int p;
        private int x;
        private int cc;
        private int m;
        private int pt;
        private int seq;
        private int ts1;
        private int ssrc;

        public Header(int v, int p, int x, int cc, int m, int pt, int seq, int ts1, int ssrc) {
            this.v = v;
            this.p = p;
            this.x = x;
            this.cc = cc;
            this.m = m;
            this.pt = pt;
            this.seq = seq;
            this.ts1 = ts1;
            this.ssrc = ssrc;
        }

        public int getV() {
            return v;
        }

        public int getP() {
            return p;
        }

        public int getX() {
            return x;
        }

        public int getCc() {
            return cc;
        }

        public int getM() {
            return m;
        }

        public int getPt() {
            return pt;
        }

        public int getSeq() {
            return seq;
        }

        public int getTs1() {
            return ts1;
        }

        public int getSsrc() {
            return ssrc;
        }
    };

    private Header header;
    private byte[] payload;

    public Packet(Header header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public Header getHeader() {
        return header;
    }

    public byte[] getPayload() {
        return payload;
    }

}
