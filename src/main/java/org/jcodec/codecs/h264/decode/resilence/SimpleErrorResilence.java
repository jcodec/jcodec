package org.jcodec.codecs.h264.decode.resilence;

import java.io.IOException;
import java.util.Collection;

import org.jcodec.codecs.h264.decode.AccessUnitReader;
import org.jcodec.codecs.h264.decode.ErrorResilence;
import org.jcodec.codecs.h264.decode.PictureDecoder;
import org.jcodec.codecs.h264.decode.SliceDecoder;
import org.jcodec.codecs.h264.decode.aso.MBlockMapper;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import org.jcodec.codecs.h264.decode.deblock.FilterParameter;
import org.jcodec.codecs.h264.decode.dpb.DecodedPicture;
import org.jcodec.codecs.h264.decode.dpb.RefListBuilder;
import org.jcodec.codecs.h264.decode.imgop.Flattener;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.DecodedSlice;
import org.jcodec.codecs.h264.io.model.CodedSlice;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A class that does simple error resilence.
 * 
 * I.e: decode primary picture; if some MBs are missing, decode next redundant
 * picture; if still some MBs are missing, decode next redundant picture; repeat
 * until there are missing MBs and there are redundant pictures available.
 * 
 * If after applying all redundant pictures some MBs are still missing they are
 * subject to error resilence. I.e. their spatial location is filled with the
 * pixels derived from the neighbouring MB borders.
 * 
 * 
 * @author Jay Codec
 * 
 */
public class SimpleErrorResilence implements ErrorResilence {
    private SeqParameterSet curSPS;
    private PictureParameterSet curPPS;

    private RefListBuilder refListBuilder;
    private SliceDecoder sliceDecoder;
    private DeblockingFilter filter;
    private Painter painter;

    public SimpleErrorResilence() {
        painter = new Painter();
    }

    public Picture decodeAccessUnit(AccessUnitReader accessUnit, DecodedPicture[] references) throws IOException {

        updateParameterSet(accessUnit);

        int picWidthInMbs = curSPS.pic_width_in_mbs_minus1 + 1;
        int picHeightInMbs = curSPS.pic_height_in_map_units_minus1 + 1;
        int picSizeInMBS = picWidthInMbs * picHeightInMbs;

        DecodedMBlock[] decoded = new DecodedMBlock[picSizeInMBS];
        SliceHeader[] headers = new SliceHeader[picSizeInMBS];

        assemblePicture(accessUnit, references, decoded, headers);

        FilterParameter[] dbfParams = PictureDecoder.buildDeblockerParams(picWidthInMbs, decoded, headers);

        suppressErrors(picWidthInMbs, picHeightInMbs, decoded, dbfParams);

        filter.applyDeblocking(decoded, dbfParams);

        Picture decPic = Flattener.flattern(decoded, picWidthInMbs, picHeightInMbs);

        return decPic;
    }

    private void updateParameterSet(AccessUnitReader accessUnit) {
        PictureParameterSet newPPS = accessUnit.getPPS();
        SeqParameterSet newSPS = accessUnit.getSPS();

        if (curSPS == null || curPPS == null || curPPS != newPPS || curSPS != newSPS) {
            curSPS = newSPS;
            curPPS = newPPS;

            int[] chromaQpOffset = new int[] {
                    curPPS.chroma_qp_index_offset,
                    curPPS.extended != null ? curPPS.extended.second_chroma_qp_index_offset
                            : curPPS.chroma_qp_index_offset };

            sliceDecoder = new SliceDecoder(curPPS.pic_init_qp_minus26 + 26, chromaQpOffset,
                    curSPS.pic_width_in_mbs_minus1 + 1, curSPS.bit_depth_luma_minus8 + 8,
                    curSPS.bit_depth_chroma_minus8 + 8, curPPS.constrained_intra_pred_flag);

            refListBuilder = new RefListBuilder(1 << (curSPS.log2_max_frame_num_minus4 + 4));
            filter = new DeblockingFilter(curSPS.pic_width_in_mbs_minus1 + 1,
                    curSPS.pic_height_in_map_units_minus1 + 1, curSPS.bit_depth_luma_minus8 + 8,
                    curSPS.bit_depth_chroma_minus8 + 8);
        }
    }

    private void assemblePicture(AccessUnitReader accessUnit, DecodedPicture[] references, DecodedMBlock[] decoded,
            SliceHeader[] headers) throws IOException {

        MapManager map = accessUnit.getMap();
        int totalMbs = 0;
        Collection<CodedSlice> pic;
        while ((pic = accessUnit.readOnePicture()) != null) {
            for (CodedSlice codedSlice : pic) {
                MBlockMapper mapper = map.getMapper(codedSlice.getHeader());
                boolean keepSlice = false;
                int[] addresses = mapper.getAddresses(codedSlice.getMblocks().length);
                for (int addr : addresses) {
                    if (decoded[addr] == null) {
                        keepSlice = true;
                        break;
                    }
                }
                if (keepSlice) {
                    Picture[] refList = refListBuilder.buildRefList(references,
                            codedSlice.getHeader().refPicReorderingL0, codedSlice.getHeader().frame_num);

                    DecodedSlice decodeSlice = sliceDecoder.decodeSlice(codedSlice, refList, mapper);

                    DecodedMBlock[] dMbs = decodeSlice.getMblocks();
                    for (int i = 0; i < dMbs.length; i++) {
                        int addr = addresses[i];
                        if (decoded[addr] == null) {
                            decoded[addr] = dMbs[i];
                            headers[addr] = codedSlice.getHeader();
                            totalMbs++;
                        }
                    }
                }
                if (totalMbs == decoded.length)
                    break;
            }
            if (totalMbs == decoded.length)
                break;
        }
    }

    private void suppressErrors(int picWidthInMbs, int picHeightInMbs, DecodedMBlock[] decoded,
            FilterParameter[] dbfParams) {
        int[] alphaBeta = new int[] { 51, 0, 0, 0 };
        int[] bs = new int[] { 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0 };
        FilterParameter.Threshold thresh = new FilterParameter.Threshold(alphaBeta, alphaBeta, alphaBeta, alphaBeta);
        FilterParameter parameter = new FilterParameter(true, true, true, thresh, thresh, thresh, bs, bs);

        int picSizeInMbs = picWidthInMbs * picHeightInMbs;

        int painted;
        do {
            painted = doOneIteration(picWidthInMbs, decoded, dbfParams, parameter, picSizeInMbs);
        } while (painted > 0);
    }

    private int doOneIteration(int picWidthInMbs, DecodedMBlock[] decoded, FilterParameter[] dbfParams,
            FilterParameter parameter, int picSizeInMbs) {
        int painted = 0;
        for (int i = 0; i < decoded.length; i++) {
            if (decoded[i] == null) {
                DecodedMBlock top = null;
                if (i >= picWidthInMbs) {
                    top = decoded[i - picWidthInMbs];
                }

                DecodedMBlock left = null;
                if ((i % picWidthInMbs) > 0) {
                    left = decoded[i - 1];
                }

                DecodedMBlock bottom = null;
                if (i < picSizeInMbs - picWidthInMbs) {
                    bottom = decoded[i + picWidthInMbs];
                }

                DecodedMBlock right = null;
                if ((i % picWidthInMbs) < picWidthInMbs - 1) {
                    right = decoded[i + 1];
                }

                if (top == null && left == null && right == null && bottom == null)
                    continue;

                decoded[i] = painter.paintMBlock(left, top, right, bottom);

                dbfParams[i] = parameter;

                if (right != null) {
                    FilterParameter rightFP = dbfParams[i + 1];
                    int[] bsV = rightFP.getBsV();
                    bsV[0] = bsV[4] = bsV[8] = bsV[12] = 4;
                    rightFP.getLumaThresh().getAlphaV()[0] = 48;
                    rightFP.getLumaThresh().getBetaV()[0] = 48;
                    rightFP.getCbThresh().getAlphaV()[0] = 36;
                    rightFP.getCbThresh().getBetaV()[0] = 36;
                    rightFP.getCrThresh().getAlphaV()[0] = 36;
                    rightFP.getCrThresh().getBetaV()[0] = 36;
                }

                if (bottom != null) {
                    FilterParameter bottomFP = dbfParams[i + picWidthInMbs];
                    int[] bsH = bottomFP.getBsH();
                    bsH[0] = bsH[4] = bsH[8] = bsH[12] = 4;
                    bottomFP.getLumaThresh().getAlphaH()[0] = 48;
                    bottomFP.getLumaThresh().getBetaH()[0] = 48;
                    bottomFP.getCbThresh().getAlphaH()[0] = 36;
                    bottomFP.getCbThresh().getBetaH()[0] = 36;
                    bottomFP.getCrThresh().getAlphaH()[0] = 36;
                    bottomFP.getCrThresh().getBetaH()[0] = 36;
                }
                ++painted;
            }
        }
        return painted;
    }
}