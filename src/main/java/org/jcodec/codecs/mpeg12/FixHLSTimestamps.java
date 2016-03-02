package org.jcodec.codecs.mpeg12;
import java.io.File;
import java.io.IOException;
import java.lang.System;
import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FixHLSTimestamps extends FixTimestamp {
    private long[] lastPts;
    
    public FixHLSTimestamps() {
        this.lastPts = new long[256];
    }
    
    public static void main1(String[] args) throws IOException {
        String wildCard = args[0];
        int startIdx = Integer.parseInt(args[1]);

        new FixHLSTimestamps().doIt(wildCard, startIdx);
    }

    private void doIt(String wildCard, int startIdx) throws IOException {
        Arrays.fill(lastPts, -1);
        for (int i = startIdx;; i++) {
            File file = new File(String.format(wildCard, i));
            System.out.println(file.getAbsolutePath());
            if (!file.exists())
                break;
            this.fix(file);
        }
    }

    protected long doWithTimestamp(int streamId, long pts, boolean isPts) {
        if (!isPts)
            return pts;
        if (lastPts[streamId] == -1) {
            lastPts[streamId] = pts;
            return pts;
        }
        if (isVideo(streamId)) {
            lastPts[streamId] += 3003;
            return lastPts[streamId];
        } else if (isAudio(streamId)) {
            lastPts[streamId] += 1920;
            return lastPts[streamId];
        }
        throw new RuntimeException("Unexpected!!!");
    }
}
