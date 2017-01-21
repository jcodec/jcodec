package org.jcodec.containers.imgseq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A demuxer that reads image files out of a folder.
 * 
 * Supports both sequences starting with 0 and 1 index.
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class ImageSequenceDemuxer implements Demuxer, DemuxerTrack {

    private static final int VIDEO_FPS = 25;
    private String namePattern;
    private int frameNo;
    private Packet curFrame;
    private Codec codec;
    private int maxAvailableFrame;
    private int maxFrames;

    public ImageSequenceDemuxer(String namePattern, int maxFrames) throws IOException {
        this.namePattern = namePattern;
        this.maxFrames = maxFrames;
        this.maxAvailableFrame = -1;
        this.curFrame = loadFrame();
        // codec = JCodecUtil.detectDecoder(curFrame.getData());
        String lowerCase = namePattern.toLowerCase();
        if (lowerCase.endsWith(".png")) {
            codec = Codec.PNG;
        } else if (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")) {
            codec = Codec.JPEG;
        }
        // Picture8Bit frame = JCodecUtil.createVideoDecoder(codec,
        // null).decodeFrame8Bit(curFrame.getData(),
        // new byte[3][1920 * 1088]);
        // dimensions = new Size(frame.getWidth(), frame.getHeight());
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        ArrayList<DemuxerTrack> tracks = new ArrayList<DemuxerTrack>();
        tracks.add(this);
        return tracks;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        return getTracks();
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        return new ArrayList<DemuxerTrack>();
    }

    @Override
    public Packet nextFrame() throws IOException {
        try {
            return curFrame;
        } finally {
            curFrame = loadFrame();
        }
    }

    private Packet loadFrame() throws IOException {
        if (frameNo > maxFrames) {
            return null;
        }

        File file = null;
        do {
            String name = String.format(namePattern, frameNo);
            file = new File(name);
            if (file.exists() || frameNo > 0)
                break;
            frameNo++;
        } while (frameNo < 2);

        if (!file.exists())
            return null;

        Packet ret = new Packet(NIOUtils.fetchFromFile(file), frameNo, VIDEO_FPS, 1, frameNo, FrameType.KEY, null, frameNo);
        ++frameNo;
        return ret;
    }

    private static final int MAX_MAX = 60 * 60 * 60 * 24; // Longest possible
                                                          // movie

    /**
     * Finds maximum frame of a sequence by bisecting the range.
     * 
     * Performs at max at max 48 Stat calls ( 2*log2(MAX_MAX) ).
     * 
     * @return
     */
    public int getMaxAvailableFrame() {
        if (maxAvailableFrame == -1) {

            int firstPoint = 0;
            for (int i = MAX_MAX; i >= 0; i /= 2) {
                if (new File(String.format(namePattern, i)).exists()) {
                    firstPoint = i;
                    break;
                }
            }
            int pos = firstPoint;
            for (int interv = firstPoint / 2; interv > 1; interv /= 2) {
                if (new File(String.format(namePattern, pos + interv)).exists()) {
                    pos += interv;
                }
            }
            maxAvailableFrame = pos;
            Logger.info("Max frame found: " + maxAvailableFrame);
        }
        return Math.min(maxAvailableFrame, maxFrames);
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        int durationFrames = getMaxAvailableFrame();
        return new DemuxerTrackMeta(TrackType.VIDEO, codec, (durationFrames + 1) * VIDEO_FPS, null, durationFrames + 1,
                null, null, null);
    }
}