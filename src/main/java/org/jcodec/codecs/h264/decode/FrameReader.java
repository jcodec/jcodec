package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Utils.unescapeNAL;
import static org.jcodec.codecs.h264.io.model.NALUnitType.IDR_SLICE;
import static org.jcodec.codecs.h264.io.model.NALUnitType.NON_IDR_SLICE;
import static org.jcodec.codecs.h264.io.model.NALUnitType.PPS;
import static org.jcodec.codecs.h264.io.model.NALUnitType.SPS;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 4 AVC ( H.264 ) Frame reader
 * 
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 * 
 * @author The JCodec project
 * 
 */
public class FrameReader {
    private IntObjectMap<SeqParameterSet> sps;
    private IntObjectMap<PictureParameterSet> pps;

    public FrameReader() {
        this.sps = new IntObjectMap<SeqParameterSet>();
        this.pps = new IntObjectMap<PictureParameterSet>();
    }

    public List<SliceReader> readFrame(List<ByteBuffer> nalUnits) {
        List<SliceReader> result = new ArrayList<SliceReader>();

        for (ByteBuffer nalData : nalUnits) {
            NALUnit nalUnit = NALUnit.read(nalData);

            unescapeNAL(nalData);
            if (SPS == nalUnit.type) {
                SeqParameterSet _sps = SeqParameterSet.read(nalData);
                sps.put(_sps.seq_parameter_set_id, _sps);
            } else if (PPS == nalUnit.type) {
                PictureParameterSet _pps = PictureParameterSet.read(nalData);
                pps.put(_pps.pic_parameter_set_id, _pps);
            } else if (IDR_SLICE == nalUnit.type || NON_IDR_SLICE == nalUnit.type) {
                if (sps.size() == 0 || pps.size() == 0) {
                    Logger.warn("Skipping frame as no SPS/PPS have been seen so far...");
                    return null;
                }
                result.add(createSliceReader(nalData, nalUnit));
            }
        }

        return result;
    }

    private SliceReader createSliceReader(ByteBuffer segment, NALUnit nalUnit) {
        BitReader _in = BitReader.createBitReader(segment);
        SliceHeaderReader shr = new SliceHeaderReader();
        SliceHeader sh = shr.readPart1(_in);
        sh.pps = pps.get(sh.pic_parameter_set_id);
        sh.sps = sps.get(sh.pps.seq_parameter_set_id);
        shr.readPart2(sh, nalUnit, sh.sps, sh.pps, _in);

        Mapper mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        CAVLC[] cavlc = new CAVLC[] { new CAVLC(sh.sps, sh.pps, 2, 2), new CAVLC(sh.sps, sh.pps, 1, 1),
                new CAVLC(sh.sps, sh.pps, 1, 1) };

        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
        CABAC cabac = new CABAC(mbWidth);

        MDecoder mDecoder = null;
        if (sh.pps.entropy_coding_mode_flag) {
            _in.terminate();
            int[][] cm = new int[2][1024];
            int qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
            cabac.initModels(cm, sh.slice_type, sh.cabac_init_idc, qp);
            mDecoder = new MDecoder(segment, cm);
        }

        return new SliceReader(sh.pps, cabac, cavlc, mDecoder, _in, mapper, sh, nalUnit);
    }
    
    public void addSpsList(List<ByteBuffer> spsList) {
        for (ByteBuffer byteBuffer : spsList) {
            addSps(byteBuffer);
        }
    }

    public void addSps(ByteBuffer byteBuffer) {
        ByteBuffer clone = NIOUtils.clone(byteBuffer);
        unescapeNAL(clone);
        SeqParameterSet s = SeqParameterSet.read(clone);
        sps.put(s.seq_parameter_set_id, s);
    }

    public void addPpsList(List<ByteBuffer> ppsList) {
        for (ByteBuffer byteBuffer : ppsList) {
            addPps(byteBuffer);
        }
    }

    public void addPps(ByteBuffer byteBuffer) {
        ByteBuffer clone = NIOUtils.clone(byteBuffer);
        unescapeNAL(clone);
        PictureParameterSet p = PictureParameterSet.read(clone);
        pps.put(p.pic_parameter_set_id, p);
    }
}