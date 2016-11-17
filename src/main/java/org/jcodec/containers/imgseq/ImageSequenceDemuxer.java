package org.jcodec.containers.imgseq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A demuxer that reads image files out of a folder.
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
    private Size dimensions;
    private int maxFrame;
    private int minFrame;

    public ImageSequenceDemuxer(File fileWithPattern) throws IOException {
        this.namePattern = fileWithPattern.getAbsolutePath();
        this.curFrame = loadFrame();
        codec = JCodecUtil.detectDecoder(curFrame.getData());
        Picture8Bit frame = JCodecUtil.createVideoDecoder(codec, null).decodeFrame8Bit(curFrame.getData(),
                new byte[3][1920 * 1088]);
        dimensions = new Size(frame.getWidth(), frame.getHeight());
        Pattern p = Pattern.compile(this.namePattern.replace("%d", "([0-9]+)"));
        minFrame = Integer.MAX_VALUE;
        for (String fileName : fileWithPattern.getParentFile().list()) {
            Matcher m = p.matcher(fileName);
            if (m.matches()) {
                int frameNo = Integer.parseInt(m.group(1));
                maxFrame = Math.max(frameNo, maxFrame);
                minFrame = Math.min(frameNo, minFrame);
            }
        }
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
        String name = String.format(namePattern, frameNo);
        File file = new File(name);
        if (!file.exists())
            return null;
        return new Packet(NIOUtils.fetchFromFile(file), frameNo, VIDEO_FPS, 1, frameNo, true, null, frameNo);
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        return new DemuxerTrackMeta(TrackType.VIDEO, codec, null, maxFrame + 1, (maxFrame + 1) * VIDEO_FPS, dimensions,
                null);
    }
}