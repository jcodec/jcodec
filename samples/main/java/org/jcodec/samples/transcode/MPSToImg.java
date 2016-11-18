package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MPEGDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.mps.MPSDemuxer;

/**
 * A profile specific to MPEG PS containers
 * 
 * @author Stanislav Vitvitskiy
 */
public abstract class MPSToImg extends ToImgProfile {
    private ThreadLocal<ByteBuffer> buffers = new ThreadLocal<ByteBuffer>();

    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) throws IOException {
        VideoDecoder decoder = JCodecUtil.createVideoDecoder(JCodecUtil.detectDecoder(firstFrame.duplicate()),
                inTrack.getMeta().getCodecPrivate());

        return decoder;
    }

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MPEGDemuxer mpsDemuxer = new MPSDemuxer(source);
        return mpsDemuxer.getVideoTracks().get(0);
    }

    @Override
    protected Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, Packet pkt) {
        return decoder.decodeFrame8Bit(pkt.getData(), target1.getData());
    }

    @Override
    protected Packet nextPacket(DemuxerTrack inTrack) throws IOException {
        ByteBuffer bb = buffers.get();
        if (bb == null) {
            bb = ByteBuffer.allocate(500 << 10);
            buffers.set(bb);
        }
        bb.clear();
        return ((MPEGDemuxerTrack) inTrack).nextFrameWithBuffer(bb);
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_PS);
    }
}
