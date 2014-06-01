package org.jcodec.codecs.h264;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts H.264 frames out H.264 Elementary stream ( according to Annex B )
 * 
 * @author Jay Codec
 * 
 */
public class MappedH264ES extends BaseH264ES implements DemuxerTrack {
    private ByteBuffer bb;

    public MappedH264ES(ByteBuffer bb) {
        this.bb = bb;
    }

    @Override
    public Packet nextFrame() {
        ByteBuffer result = bb.duplicate();

        NALUnit prevNu = null;
        SliceHeader prevSh = null;
        while (true) {
            bb.mark();
            ByteBuffer buf = H264Utils.nextNALUnit(bb);
            if (buf == null)
                break;
            // NIOUtils.skip(buf, 4);
            NALUnit nu = NALUnit.read(buf);

            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
                SliceHeader sh = readSliceHeader(buf, nu);

                if (prevNu != null && prevSh != null && !sameFrame(prevNu, nu, prevSh, sh)) {
                    bb.reset();
                    break;
                }
                prevSh = sh;
                prevNu = nu;
            } else if (nu.type == NALUnitType.PPS) {
                PictureParameterSet read = PictureParameterSet.read(buf);
                pps.put(read.pic_parameter_set_id, read);
            } else if (nu.type == NALUnitType.SPS) {
                SeqParameterSet read = SeqParameterSet.read(buf);
                sps.put(read.seq_parameter_set_id, read);
            }
        }

        result.limit(bb.position());

        return prevSh == null ? null : detectPoc(result, prevNu, prevSh);
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        throw new UnsupportedOperationException();
    }
}