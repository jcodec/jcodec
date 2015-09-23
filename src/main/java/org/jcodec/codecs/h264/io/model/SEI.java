package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Supplementary Enhanced Information entity of H264 bitstream
 * 
 * capable to serialize and deserialize with CAVLC bitstream
 * 
 * @author The JCodec project
 * 
 */
public class SEI {

    public static class SEIMessage {
        public int payloadType;
        public int payloadSize;
        public byte[] payload;

        public SEIMessage(int payloadType2, int payloadSize2, byte[] payload2) {
            this.payload = payload2;
            this.payloadType = payloadType2;
            this.payloadSize = payloadSize2;
        }

    }

    public SEIMessage[] messages;

    public SEI(SEIMessage[] messages) {
        this.messages = messages;
    }

    public static SEI read(ByteBuffer is) {

        List<SEIMessage> messages = new ArrayList<SEIMessage>();
        SEIMessage msg;
        do {
            msg = sei_message(is);
            if (msg != null)
                messages.add(msg);
        } while (msg != null);

        return new SEI((SEIMessage[]) messages.toArray(new SEIMessage[] {}));
    }

    private static SEIMessage sei_message(ByteBuffer is) {
        int payloadType = 0;
        int b = 0;
        while (is.hasRemaining() && ( b = (is.get() & 0xff)) == 0xff) {
            payloadType += 255;
        }
        if (!is.hasRemaining())
            return null;
        payloadType += b;
        int payloadSize = 0;
        while (is.hasRemaining() && (b = (is.get() & 0xff)) == 0xff) {
            payloadSize += 255;
        }
        if (!is.hasRemaining())
            return null;
        payloadSize += b;
        byte[] payload = sei_payload(payloadType, payloadSize, is);
        if (payload.length != payloadSize)
            return null;

        return new SEIMessage(payloadType, payloadSize, payload);

    }

    private static byte[] sei_payload(int payloadType, int payloadSize, ByteBuffer is) {
        byte[] res = new byte[payloadSize];
        is.get(res);
        return res;
    }

    public void write(ByteBuffer out) {
        BitWriter writer = new BitWriter(out);
        // TODO Auto-generated method stub

        writeTrailingBits(writer);
    }
}