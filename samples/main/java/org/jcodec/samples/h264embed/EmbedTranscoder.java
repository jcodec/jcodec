package org.jcodec.samples.h264embed;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class EmbedTranscoder {
    private TIntObjectHashMap<SeqParameterSet> sps = new TIntObjectHashMap<SeqParameterSet>();
    private TIntObjectHashMap<PictureParameterSet> pps = new TIntObjectHashMap<PictureParameterSet>();

    public ByteBuffer transcode(ByteBuffer data, ByteBuffer _out) {
        return transcode(H264Utils.splitFrame(data), _out);
    }
    
    public ByteBuffer transcode(List<ByteBuffer> data, ByteBuffer _out) {
        for (ByteBuffer nalUnit : data) {
            NIOUtils.skip(nalUnit, 4);
            NALUnit marker = NALUnit.read(nalUnit);
            _out.putInt(1);
            marker.write(_out);
            if (marker.type == NALUnitType.NON_IDR_SLICE || marker.type == NALUnitType.IDR_SLICE) {
                transcodeSlice(nalUnit, marker);
            } else {
                if (marker.type == NALUnitType.SPS) {
                    SeqParameterSet _sps = SeqParameterSet.read(nalUnit.duplicate());
                    sps.put(_sps.seqParameterSetId, _sps);
                } else if (marker.type == NALUnitType.PPS) {
                    PictureParameterSet _pps = PictureParameterSet.read(nalUnit.duplicate());
                    pps.put(_pps.picParameterSetId, _pps);
                }
                NIOUtils.write(_out, nalUnit);
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