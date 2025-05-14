package org.jcodec.codecs.mpeg4.es;

import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import static org.jcodec.common.Preconditions.checkState;

public class DescriptorParser {

    private final static int ES_TAG = 0x03;
    private final static int DC_TAG = 0x04;
    private final static int DS_TAG = 0x05;
    private final static int SL_TAG = 0x06;

    public static Descriptor read(ByteBuffer input) {
        if (input.remaining() < 2)
            return null;
        int tag = input.get() & 0xff;
        int size = JCodecUtil2.readBER32(input);

        ByteBuffer byteBuffer = NIOUtils.read(input, size);

        switch (tag) {
            case ES_TAG:
                return parseES(byteBuffer);
            case SL_TAG:
                return parseSL(byteBuffer);
            case DC_TAG:
                return parseDecoderConfig(byteBuffer);
            case DS_TAG:
                return parseDecoderSpecific(byteBuffer);
            default:
                throw new RuntimeException("unknown tag "+tag);
        }
    }

    @UsedViaReflection
    private static NodeDescriptor parseNodeDesc(ByteBuffer input) {
        Collection<Descriptor> children = new ArrayList<Descriptor>();
        Descriptor d;
        do {
            d = read(input);
            if (d != null)
                children.add(d);
        } while (d != null);
        return new NodeDescriptor(0, children);
    }

    private static ES parseES(ByteBuffer input) {
        int trackId = input.getShort();
        input.get();
        NodeDescriptor node = parseNodeDesc(input);
        return new ES(trackId, node.getChildren());
    }

    private static SL parseSL(ByteBuffer input) {
        int state = input.get() & 0xff;
        checkState(0 == state || 0x2 == state);
        return new SL();
    }

    private static DecoderSpecific parseDecoderSpecific(ByteBuffer input) {
        ByteBuffer data = NIOUtils.readBuf(input);
        return new DecoderSpecific(data);
    }

    private static DecoderConfig parseDecoderConfig(ByteBuffer input) {
        int objectType = input.get() & 0xff;
        input.get();
        int bufSize = ((input.get() & 0xff) << 16) | (input.getShort() & 0xffff);
        int maxBitrate = input.getInt();
        int avgBitrate = input.getInt();

        NodeDescriptor node = parseNodeDesc(input);
        return new DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, node.getChildren());
    }
}
