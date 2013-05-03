package org.jcodec.samples.h264embed;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.NIOUtils;

public class EmbedTranscoder {
    private TIntObjectHashMap<SeqParameterSet> sps = new TIntObjectHashMap<SeqParameterSet>();
    private TIntObjectHashMap<PictureParameterSet> pps = new TIntObjectHashMap<PictureParameterSet>();

    public ByteBuffer transcode(ByteBuffer data, ByteBuffer _out) {
        ByteBuffer segment;
        while ((segment = H264Utils.nextNALUnit(data)) != null) {
            NIOUtils.skip(segment, 4);
            NALUnit marker = NALUnit.read(segment);
            _out.putInt(1);
            marker.write(_out);
            if (marker.type == NALUnitType.NON_IDR_SLICE || marker.type == NALUnitType.IDR_SLICE) {
                transcodeSlice(segment, marker);
            } else {
                if (marker.type == NALUnitType.SPS) {
                    SeqParameterSet _sps = SeqParameterSet.read(segment.duplicate());
                    sps.put(_sps.seq_parameter_set_id, _sps);
                } else if (marker.type == NALUnitType.PPS) {
                    PictureParameterSet _pps = PictureParameterSet.read(segment.duplicate());
                    pps.put(_pps.pic_parameter_set_id, _pps);
                }
                NIOUtils.write(_out, segment);
            }
        }

        _out.flip();
        return _out;
    }

    private void transcodeSlice(ByteBuffer segment, NALUnit marker) {
        mbPassThrough();
        mbTranscode();
    }

    private void mbTranscode() {
        // TODO Auto-generated method stub
        
    }

    private void mbPassThrough() {
        // TODO Auto-generated method stub
        
    }
}