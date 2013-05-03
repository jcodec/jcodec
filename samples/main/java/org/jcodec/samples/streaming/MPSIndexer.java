package org.jcodec.samples.streaming;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MPSDemuxer.MPEGPacket;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.jcodec.containers.mps.MPSDemuxer.Track;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates an index of an MPEG PS file
 * 
 * @author The JCodec project
 * 
 */
public class MPSIndexer {

    private File mtsFile;
    private MTSIndex index;
    private volatile boolean done;

    public MPSIndexer(File mtsFile, MTSIndex index) {
        this.mtsFile = mtsFile;
        this.index = index;
    }

    public void index() throws IOException {
        SeekableByteChannel channel = null;
        try {
            channel = new FileChannelWrapper(mtsFile);
            MPSDemuxer demuxer = new MPSDemuxer(channel);
            while (true) {
                for (Track track : demuxer.getVideoTracks()) {
                    MPEGPacket frame = track.getFrame(null);
                    if (frame == null)
                        break;
                    index.addVideo(track.getSid(), frame.getOffset(), frame.getPts(), (int) frame.getDuration(),
                            frame.getSeq(), frame.getGOP(), frame.getTimecode(), (short)frame.getDisplayOrder(),
                            (byte)(frame.isKeyFrame() ? 0 : 1));
                }
                for (Track track : demuxer.getAudioTracks()) {
                    List<PESPacket> pending = track.getPending();
                    for (PESPacket pesPacket : pending) {
                        index.addAudio(track.getSid(), pesPacket.pos, pesPacket.pts, 0);
                    }
                }
            }
        } finally {
            channel.close();
            done = true;
        }
    }

    public boolean isDone() {
        return done;
    }
}