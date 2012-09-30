package org.jcodec.codecs.h264.decode;

import java.io.IOException;

import org.jcodec.codecs.h264.decode.aso.MBlockMapper;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import org.jcodec.codecs.h264.decode.deblock.FilterParameter;
import org.jcodec.codecs.h264.decode.deblock.FilterParameterBuilder;
import org.jcodec.codecs.h264.decode.dpb.DecodedPicture;
import org.jcodec.codecs.h264.decode.dpb.RefListBuilder;
import org.jcodec.codecs.h264.decode.imgop.Flattener;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.DecodedSlice;
import org.jcodec.codecs.h264.io.model.CodedPicture;
import org.jcodec.codecs.h264.io.model.CodedSlice;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decoder for an individual picture
 * 
 * @author Jay Codec
 * 
 */
public class PictureDecoder {
    private SliceDecoder sliceDecoder;
    private SeqParameterSet sps;
    private RefListBuilder refListBuilder;
    private DeblockingFilter filter;

    public PictureDecoder(SeqParameterSet sps, PictureParameterSet pps) {

        this.sps = sps;

        int[] chromaQpOffset = new int[] { pps.chroma_qp_index_offset,
                pps.extended != null ? pps.extended.second_chroma_qp_index_offset : pps.chroma_qp_index_offset };

        sliceDecoder = new SliceDecoder(pps.pic_init_qp_minus26 + 26, chromaQpOffset, sps.pic_width_in_mbs_minus1 + 1,
                sps.bit_depth_luma_minus8 + 8, sps.bit_depth_chroma_minus8 + 8, pps.constrained_intra_pred_flag);

        refListBuilder = new RefListBuilder(1 << (sps.log2_max_frame_num_minus4 + 4));

        filter = new DeblockingFilter(sps.pic_width_in_mbs_minus1 + 1, sps.pic_height_in_map_units_minus1 + 1,
                sps.bit_depth_luma_minus8 + 8, sps.bit_depth_chroma_minus8 + 8);

    }

    public Picture decodePicture(CodedPicture coded, MapManager map, DecodedPicture[] references) throws IOException {

        int picWidthInMbs = sps.pic_width_in_mbs_minus1 + 1;
        int picHeightInMbs = sps.pic_height_in_map_units_minus1 + 1;
        int mblocksInFrame = picWidthInMbs * picHeightInMbs;

        DecodedMBlock[] mblocks = new DecodedMBlock[mblocksInFrame];
        SliceHeader[] headers = new SliceHeader[mblocksInFrame];

        for (CodedSlice codedSlice : coded.getSlices()) {

            if (codedSlice.getHeader().redundant_pic_cnt != 0)
                continue;

            MBlockMapper mBlockMap = map.getMapper(codedSlice.getHeader());

            Picture[] refList = refListBuilder.buildRefList(references, codedSlice.getHeader().refPicReorderingL0,
                    codedSlice.getHeader().frame_num);

            DecodedSlice decodeSlice = sliceDecoder.decodeSlice(codedSlice, refList, mBlockMap);

            mapDecodedMBlocks(decodeSlice, codedSlice, headers, mblocks, mBlockMap);
        }

        FilterParameter[] dbfInput = buildDeblockerParams(picWidthInMbs, mblocks, headers);

        filter.applyDeblocking(mblocks, dbfInput);

        Picture pic = Flattener.flattern(mblocks, picWidthInMbs, picHeightInMbs);

        return pic;
    }

    public static FilterParameter[] buildDeblockerParams(int picWidthInMbs, DecodedMBlock[] decoded,
            SliceHeader[] headers) {

        FilterParameter[] result = new FilterParameter[decoded.length];

        for (int i = 0; i < decoded.length; i++) {

            SliceHeader header = headers[i];

            if (header == null)
                continue;

            DecodedMBlock leftDec = null;
            SliceHeader leftHead = null;
            if ((i % picWidthInMbs) > 0) {
                leftDec = decoded[i - 1];
                leftHead = headers[i - 1];
            }

            DecodedMBlock topDec = null;
            SliceHeader topHead = null;
            if (i >= picWidthInMbs) {
                topDec = decoded[i - picWidthInMbs];
                topHead = headers[i - picWidthInMbs];
            }

            result[i] = FilterParameterBuilder.calcParameterForMB(header.disable_deblocking_filter_idc,
                    header.slice_alpha_c0_offset_div2 << 1, header.slice_beta_offset_div2 << 1, decoded[i], leftDec,
                    topDec, leftHead == header, topHead == header);
        }

        return result;
    }

    protected static void mapDecodedMBlocks(DecodedSlice slice, CodedSlice coded, SliceHeader[] headers,
            DecodedMBlock[] mblocks, MBlockMapper mBlockMap) {

        DecodedMBlock[] sliceMBlocks = slice.getMblocks();
        SliceHeader sliceHeader = coded.getHeader();

        int[] addresses = mBlockMap.getAddresses(sliceMBlocks.length);

        for (int i = 0; i < sliceMBlocks.length; i++) {
            int addr = addresses[i];
            mblocks[addr] = sliceMBlocks[i];
            headers[addr] = sliceHeader;
        }
    }
}
