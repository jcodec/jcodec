package org.jcodec.samples.streaming;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.readableFileChannel;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mps.MPEGDemuxer;
import org.jcodec.containers.mps.MPEGPacket;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.PESPacket;

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
            channel = readableChannel(mtsFile);
            MPSDemuxer demuxer = new MPSDemuxer(channel);
            while (true) {
                for (MPEGDemuxer.MPEGDemuxerTrack track : demuxer.getVideoTracks()) {
                    MPEGPacket frame = (MPEGPacket) track.nextFrameWithBuffer(null);
                    if (frame == null)
                        break;
                    index.addVideo(track.getSid(), frame.getOffset(), frame.getPts(), (int) frame.getDuration(),
                            frame.getSeq(), frame.getGOP(), frame.getTimecode(), (short)frame.getDisplayOrder(),
                            (byte)(frame.isKeyFrame() ? 0 : 1));
                }
                for (MPEGDemuxer.MPEGDemuxerTrack track : demuxer.getAudioTracks()) {
                    List<PESPacket> pending = track.getPending();
                    for (PESPacket pesPacket : pending) {
                        index.addAudio(track.getSid(), pesPacket.pos, pesPacket.pts, 0);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(channel);
            done = true;
        }
    }

    public boolean isDone() {
        return done;
    }
}