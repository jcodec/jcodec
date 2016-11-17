package org.jcodec.samples.transcode;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;

/**
 * Profile specific to MPEG TS containers
 * @author Stanislav Vitvitskiy
 *
 */
public abstract class MTSToImg extends MPSToImg {
	
	@Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        Set<Integer> programs = MTSDemuxer.getProgramsFromChannel(source);
        for (int program : programs) {
            System.out.println("Transcoding program " + String.format("%x", program));
            source.setPosition(0);
            MTSDemuxer mts = new MTSDemuxer(source, program);
            // We ignore all audio tracks
            for (MPEGDemuxerTrack track : mts.getAudioTracks()) {
                track.ignore();
            }
            List<? extends MPEGDemuxerTrack> videoTracks = mts.getVideoTracks();
            if (videoTracks.size() == 0)
                continue;
            MPEGDemuxerTrack videoTrack = videoTracks.remove(0);
            // We ignore all video tracks but the first
            for (MPEGDemuxerTrack track : videoTracks) {
                track.ignore();
            }
            return videoTrack;
        }
        return null;
    }
	
	@Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_TS);
    }
}
