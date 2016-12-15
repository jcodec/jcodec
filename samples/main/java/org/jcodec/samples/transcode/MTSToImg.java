package org.jcodec.samples.transcode;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.mps.MPSDemuxer;

/**
 * Profile specific to MPEG TS containers
 * 
 * @author Stanislav Vitvitskiy
 *
 */
public abstract class MTSToImg extends MPSToImg {

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MTSDemuxer mts = new MTSDemuxer(source);
        DemuxerTrack videoTrack = null;
        for (int program : mts.getPrograms()) {
            if(videoTrack != null) {
                // Close the programs we don't need
                NIOUtils.closeQuietly(mts.getProgram(program));
            }
            System.out.println("Transcoding program " + String.format("%x", program));
            MPSDemuxer mps = new MPSDemuxer(mts.getProgram(program));
            // We ignore all audio tracks
            for (MPEGDemuxerTrack track : mps.getAudioTracks()) {
                track.ignore();
            }
            List<? extends MPEGDemuxerTrack> videoTracks = mps.getVideoTracks();
            if (videoTracks.size() == 0)
                continue;
            videoTrack = videoTracks.get(0);
            // We ignore all video tracks but the first
            for (MPEGDemuxerTrack track : videoTracks) {
                if(track != videoTrack) {
                    // Ignore the streams we don't need
                    track.ignore();
                }
            }
        }
        return videoTrack;
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_TS);
    }
}
