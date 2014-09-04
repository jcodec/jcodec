package org.jcodec.codecs.aac.blocks;

import java.io.IOException;

import org.jcodec.codecs.aac.ChannelPosition;
import org.jcodec.common.io.BitReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decode program configuration element; reference: table 4.2.
 * 
 * @author The JCodec project
 * 
 */
public class BlockPCE extends Block {

    private static final int MAX_ELEM_ID = 16;

    public static class ChannelMapping {
        RawDataBlockType syn_ele;
        int someInt;
        ChannelPosition position;
    }

    public void parse(BitReader in) {

        in.readNBit(2); // object_type

        int samplingIndex = in.readNBit(4);

        int num_front = in.readNBit(4);
        int num_side = in.readNBit(4);
        int num_back = in.readNBit(4);
        int num_lfe = in.readNBit(2);
        int num_assoc_data = in.readNBit(3);
        int num_cc = in.readNBit(4);

        if (in.read1Bit() != 0)
            in.readNBit(4); // mono_mixdown_tag
        if (in.read1Bit() != 0)
            in.readNBit(4); // stereo_mixdown_tag

        if (in.read1Bit() != 0)
            in.readNBit(3); // mixdown_coeff_index and pseudo_surround

        // if (!in.moreData(4 * (num_front + num_side + num_back + num_lfe +
        // num_assoc_data + num_cc))) {
        // throw new RuntimeException("Overread");
        // }
        ChannelMapping[] layout_map = new ChannelMapping[MAX_ELEM_ID * 4];

        int tags = 0;
        decodeChannelMap(layout_map, tags, ChannelPosition.AAC_CHANNEL_FRONT, in, num_front);
        tags = num_front;
        decodeChannelMap(layout_map, tags, ChannelPosition.AAC_CHANNEL_SIDE, in, num_side);
        tags += num_side;
        decodeChannelMap(layout_map, tags, ChannelPosition.AAC_CHANNEL_BACK, in, num_back);
        tags += num_back;
        decodeChannelMap(layout_map, tags, ChannelPosition.AAC_CHANNEL_LFE, in, num_lfe);
        tags += num_lfe;

        in.skip(4 * num_assoc_data);

        decodeChannelMap(layout_map, tags, ChannelPosition.AAC_CHANNEL_CC, in, num_cc);
        tags += num_cc;

        in.align();

        /* comment field, first byte is length */
        int comment_len = in.readNBit(8) * 8;
        // if (!in.moreData(comment_len)) {
        // throw new RuntimeException("Overread");
        // }
        in.skip(comment_len);
    }

    /**
     * Decode an array of 4 bit element IDs, optionally interleaved with a
     * stereo/mono switching bit.
     * 
     * @throws IOException
     */
    private void decodeChannelMap(ChannelMapping layout_map[], int offset, ChannelPosition type, BitReader in, int n) {
        while (n-- > 0) {
            RawDataBlockType syn_ele = null;
            switch (type) {
            case AAC_CHANNEL_FRONT:
            case AAC_CHANNEL_BACK:
            case AAC_CHANNEL_SIDE:
                syn_ele = RawDataBlockType.fromOrdinal(in.read1Bit());
                break;
            case AAC_CHANNEL_CC:
                in.read1Bit();
                syn_ele = RawDataBlockType.TYPE_CCE;
                break;
            case AAC_CHANNEL_LFE:
                syn_ele = RawDataBlockType.TYPE_LFE;
                break;
            }
            layout_map[offset].syn_ele = syn_ele;
            layout_map[offset].someInt = in.readNBit(4);
            layout_map[offset].position = type;
            offset++;
        }
    }
}