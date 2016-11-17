package org.jcodec.samples.transcode;

import java.util.Set;

import org.jcodec.common.Codec;

class TsAvc2Png extends MTSToImg {
    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
    }
}