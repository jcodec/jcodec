package org.jcodec.codecs.mpeg12;
import js.io.File;
import js.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SetBaseTimestamp extends FixTimestamp {
    private int baseTs;
    private long firstPts = -1;
    private boolean video;

    public SetBaseTimestamp(boolean video, int baseTs) {
        this.video = video;
        this.baseTs = baseTs;
    }

    public static void main1(String[] args) throws IOException {
        File file = new File(args[0]);
        new SetBaseTimestamp("video".equalsIgnoreCase(args[1]), Integer.parseInt(args[2])).fix(file);
    }

    protected long doWithTimestamp(int streamId, long pts, boolean isPts) {
        if (this.video && isVideo(streamId) || !this.video && isAudio(streamId)) {
            if (firstPts == -1)
                firstPts = pts;

            return pts - firstPts + baseTs;
        } else
            return pts;
    }
}