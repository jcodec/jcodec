package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Supplementary Enhanced Information entity of H264 bitstream
 * 
 * capable to serialize and deserialize with CAVLC bitstream
 * 
 * @author Jay Codec
 * 
 */
public class SEI extends BitstreamElement {

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

    public static SEI read(InputStream is) throws IOException {

        List<SEIMessage> messages = new ArrayList<SEIMessage>();
        SEIMessage msg;
        do {
            msg = sei_message(is);
            if (msg != null)
                messages.add(msg);
        } while (msg != null);

        return new SEI((SEIMessage[]) messages.toArray(new SEIMessage[] {}));
    }

    private static SEIMessage sei_message(InputStream is) throws IOException {
        int payloadType = 0;
        int b;
        while ((b = is.read()) == 0xff) {
            payloadType += 255;
        }
        if (b == -1)
            return null;
        payloadType += b;
        int payloadSize = 0;
        while ((b = is.read()) == 0xff) {
            payloadSize += 255;
        }
        if (b == -1)
            return null;
        payloadSize += b;
        byte[] payload = sei_payload(payloadType, payloadSize, is);
        if (payload.length != payloadSize)
            return null;

        return new SEIMessage(payloadType, payloadSize, payload);

    }

    private static byte[] sei_payload(int payloadType, int payloadSize, InputStream is) throws IOException {
        byte[] res = new byte[payloadSize];
        return is.read(res) == payloadSize ? res : null;
    }

    public void write(OutputStream out) throws IOException {
        OutBits writer = new BitstreamWriter(out);
        // TODO Auto-generated method stub

        writeTrailingBits(writer);
    }
}