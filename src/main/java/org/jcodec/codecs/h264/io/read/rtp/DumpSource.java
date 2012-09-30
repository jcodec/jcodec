package org.jcodec.codecs.h264.io.read.rtp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.codecs.h264.io.read.rtp.Packet.Header;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A source that reads dumped RTP packets
 * 
 * @author Jay Codec
 * 
 */
public class DumpSource implements PacketSource {

    private DataInputStream is;

    public DumpSource(InputStream is) {
        super();
        this.is = new DataInputStream(is);
    }

    public Packet getNextPacket() throws IOException {
        byte[] buf = new byte[8];
        int read = is.read(buf);
        if (read == -1)
            return null;
        int packetLen = ((buf[3] & 0xff) << 24) + ((buf[2] & 0xff) << 16) + ((buf[1] & 0xff) << 8) + (buf[0] & 0xff);

        int b1 = is.read();
        int v = (b1 >> 6) & 0x03;
        int p = (b1 >> 5) & 0x01;
        int x = (b1 >> 4) & 0x01;
        int cc = (b1 >> 0) & 0x0F;

        int b2 = is.read();
        int m = (b2 >> 7) & 0x01;
        int pt = (b2 >> 0) & 0x7F;

        int seq = is.readShort();

        int ts1 = is.readInt();
        int ssrc = is.readInt();

        byte[] payload = readSure(packetLen - 12);

        // header consistency checks
        if ((v != 2) || (p != 0) || (x != 0) || (cc != 0)) {
            throw new IOException("RTP header consistency problem");
        }

        return new Packet(new Header(v, p, x, cc, m, pt, seq, ts1, ssrc), payload);
    }

    private byte[] readSure(int packetLen) throws IOException {
        byte[] payload = new byte[packetLen];
        int read = 0;
        while (read < packetLen)
            read += is.read(payload, read, packetLen - read);
        return payload;
    }
}
