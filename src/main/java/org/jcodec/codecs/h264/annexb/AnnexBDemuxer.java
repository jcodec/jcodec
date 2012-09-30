package org.jcodec.codecs.h264.annexb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.AccessUnit;
import org.jcodec.codecs.h264.H264Demuxer;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.read.SliceHeaderReader;
import org.jcodec.common.io.BitstreamReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts an access unit coded as a sequence of NAL unit in Annex B format
 * 
 * @author Jay Codec
 * 
 */
public class AnnexBDemuxer implements H264Demuxer {
    private Map<Integer, SeqParameterSet> spsSet = new HashMap<Integer, SeqParameterSet>();
    private Map<Integer, PictureParameterSet> ppsSet = new HashMap<Integer, PictureParameterSet>();

    private SliceHeaderReader sliceHeaderReader;
    private NALUnitSource nuSource;
    private byte[] currentUnit;

    private class AnnexBAccessUnit implements AccessUnit {
        private NALUnit oldNu;
        private SliceHeader oldSh;

        public InputStream nextNALUnit() throws IOException {
            InputStream is;
            while ((is = nextNALUnitSub()) == NON_SLICE)
                ;

            return is;
        }

        public InputStream nextNALUnitSub() throws IOException {
            if (currentUnit == null)
                return null;

            ByteArrayInputStream stream = new ByteArrayInputStream(currentUnit);

            NALUnit nu = NALUnit.read(stream);

            if (nu.type != NALUnitType.IDR_SLICE && nu.type != NALUnitType.NON_IDR_SLICE) {
                processNonPictureUnit(nu, stream);
                currentUnit = readNextUnit();
                return NON_SLICE;
            }

            SliceHeader sh = sliceHeaderReader.read(nu, new BitstreamReader(stream));

            if (oldNu != null && oldSh != null && !sameAccessUnit(oldNu, nu, oldSh, sh)) {
                return null;
            }

            oldNu = nu;
            oldSh = sh;

            stream.reset();

            currentUnit = readNextUnit();

            return stream;
        }
    }

    public AnnexBDemuxer(InputStream src) throws IOException {
        this(src, new NALUnitReader(src));
    }

    public AnnexBDemuxer(InputStream src, NALUnitSource nuSource) throws IOException {

        sliceHeaderReader = new SliceHeaderReader(this);
        this.nuSource = nuSource;

        currentUnit = readNextUnit();
    }

    private final InputStream NON_SLICE = new InputStream() {
        public int read() throws IOException {
            return 0;
        }
    };

    private byte[] readNextUnit() throws IOException {
        InputStream is = nuSource.nextNALUnit();
        if (is != null)
            return IOUtils.toByteArray(is);
        return null;
    }

    private void processNonPictureUnit(NALUnit nu, InputStream is) throws IOException {
        if (nu.type == NALUnitType.SPS) {
            SeqParameterSet sps = SeqParameterSet.read(is);
            spsSet.put(sps.seq_parameter_set_id, sps);
        } else if (nu.type == NALUnitType.PPS) {
            PictureParameterSet pps = PictureParameterSet.read(is);
            ppsSet.put(pps.pic_parameter_set_id, pps);
        }

    }

    private boolean sameAccessUnit(NALUnit nu1, NALUnit nu2, SliceHeader sh1, SliceHeader sh2) {
        if (sh1.pic_parameter_set_id != sh2.pic_parameter_set_id)
            return false;

        if (sh1.frame_num != sh2.frame_num)
            return false;

        PictureParameterSet pps = ppsSet.get(sh1.pic_parameter_set_id);
        SeqParameterSet sps = spsSet.get(pps.seq_parameter_set_id);

        if ((sps.pic_order_cnt_type == 0 && sh1.pic_order_cnt_lsb != sh2.pic_order_cnt_lsb))
            return false;

        if ((sps.pic_order_cnt_type == 1 && (sh1.delta_pic_order_cnt[0] != sh2.delta_pic_order_cnt[0] || sh1.delta_pic_order_cnt[1] != sh2.delta_pic_order_cnt[1])))
            return false;

        if (((nu1.nal_ref_idc == 0 || nu2.nal_ref_idc == 0) && nu1.nal_ref_idc != nu2.nal_ref_idc))
            return false;

        if (((nu1.type == NALUnitType.IDR_SLICE) != (nu2.type == NALUnitType.IDR_SLICE)))
            return false;

        if (sh1.idr_pic_id != sh2.idr_pic_id)
            return false;

        return true;
    }

    public AccessUnit nextAcceessUnit() {
        if (currentUnit != null)
            return new AnnexBAccessUnit();

        return null;
    }

    public interface NALUnitSource {
        InputStream nextNALUnit() throws IOException;
    }

    public PictureParameterSet getPPS(int id) {
        return ppsSet.get(id);
    }

    public SeqParameterSet getSPS(int id) {
        return spsSet.get(id);
    };
}