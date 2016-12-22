package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MPEGDemuxer;
import org.jcodec.containers.mps.MPSDemuxer;

/**
 * A profile specific to MPEG PS containers
 * 
 * @author Stanislav Vitvitskiy
 */
public abstract class MPSToImg extends ToImgTranscoder {
    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) {
        DemuxerTrackMeta meta = inTrack.getMeta();
        VideoDecoder decoder = JCodecUtil.createVideoDecoder(JCodecUtil.detectDecoder(firstFrame.duplicate()),
                meta == null ? null : meta.getCodecPrivate());

        return decoder;
    }

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MPEGDemuxer mpsDemuxer = new MPSDemuxer(source);
        return mpsDemuxer.getVideoTracks().get(0);
    }

    @Override
    protected Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, ByteBuffer pkt) {
        return decoder.decodeFrame8Bit(pkt, target1.getData());
    }

    @Override
    protected Packet nextPacket(DemuxerTrack inTrack) throws IOException {
        return inTrack.nextFrame();
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_PS);
    }
}
