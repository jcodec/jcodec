package org.jcodec.codecs.h264.decode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.jcodec.codecs.h264.AccessUnit;
import org.jcodec.codecs.h264.StreamParams;
import org.jcodec.codecs.h264.decode.aso.MBlockMapper;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.io.model.CodedSlice;
import org.jcodec.codecs.h264.io.model.Macroblock;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.read.CAVLCReader;
import org.jcodec.codecs.h264.io.read.SliceDataReader;
import org.jcodec.codecs.h264.io.read.SliceHeaderReader;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Interprests nal units as a sequence of access units
 * 
 * @author Jay Codec
 * 
 */
public class AccessUnitReader {

    private SliceHeader firstSh;
    private NALUnit firstNu;
    private FetchedSlice curSlice;
    private MapManager map;
    private PictureParameterSet pps;
    private SeqParameterSet sps;
    private AccessUnit au;
    private SliceHeaderReader headerReader;
    private SliceDataReader dataReader;

    public AccessUnitReader(AccessUnit au, StreamParams streamParams) throws IOException {
        headerReader = new SliceHeaderReader(streamParams);

        this.au = au;

        this.curSlice = fetchSlice();

        firstSh = curSlice.sh;
        firstNu = curSlice.nu;

        pps = firstSh.pps;
        sps = firstSh.sps;

        map = new MapManager(sps, pps);

        dataReader = new SliceDataReader(pps.extended != null ? pps.extended.transform_8x8_mode_flag : false,
                sps.chroma_format_idc, pps.entropy_coding_mode_flag, sps.mb_adaptive_frame_field_flag,
                sps.frame_mbs_only_flag, pps.num_slice_groups_minus1 + 1, sps.bit_depth_luma_minus8 + 8,
                sps.bit_depth_chroma_minus8 + 8, pps.num_ref_idx_l0_active_minus1 + 1,
                pps.num_ref_idx_l1_active_minus1 + 1, pps.constrained_intra_pred_flag);
    }

    static int iii = 0;

    private FetchedSlice fetchSlice() throws IOException {
        InputStream is = au.nextNALUnit();

        if (is == null)
            return null;

        NALUnit nu = NALUnit.read(is);

        InBits in = new BitstreamReader(is);
        SliceHeader sh = headerReader.read(nu, in);

        return new FetchedSlice(in, nu, sh);
    }

    public Collection<CodedSlice> readOnePicture() throws IOException {

        Collection<CodedSlice> slices = new ArrayList<CodedSlice>();

        while (curSlice != null && firstSh.redundant_pic_cnt == curSlice.sh.redundant_pic_cnt) {

            MBlockMapper mBlockMap = map.getMapper(curSlice.sh);

            Macroblock[] macroblocks = dataReader.read(curSlice.in, curSlice.sh, mBlockMap);

            CAVLCReader.readTrailingBits(curSlice.in);

            slices.add(new CodedSlice(curSlice.sh, macroblocks));

            curSlice = fetchSlice();
        }

        return slices.size() == 0 ? null : slices;
    }

    public void finish() throws IOException {
        while (curSlice != null) {
            curSlice = fetchSlice();
        }
    }

    public SliceHeader getSliceHeader() {
        return firstSh;
    }

    public NALUnit getNU() {
        return firstNu;
    }

    public MapManager getMap() {
        return map;
    }

    public PictureParameterSet getPPS() {
        return pps;
    }

    public SeqParameterSet getSPS() {
        return sps;
    }
};

class FetchedSlice {
    InBits in;
    NALUnit nu;
    SliceHeader sh;

    public FetchedSlice(InBits in, NALUnit nu, SliceHeader sh) {
        this.in = in;
        this.nu = nu;
        this.sh = sh;
    }
};