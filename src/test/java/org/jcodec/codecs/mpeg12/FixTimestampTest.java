package org.jcodec.codecs.mpeg12;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.junit.Test;

public class FixTimestampTest {

    public static class SetPTS extends FixTimestamp {
        long delta = Long.MIN_VALUE;
        private long setpts;

        public SetPTS(long setpts) {
            this.setpts = setpts;
        }

        protected long doWithTimestamp(int streamId, long pts, boolean isPts) {
<<<<<<< HEAD
            if (!isPts)
                return pts;
=======
//            if (!isPts)
//                return pts;
>>>>>>> master
            if (delta == Long.MIN_VALUE) {
                delta = setpts - pts;
            }

            return pts + delta;
        }

    }

    public static void setFirstPts(File tsfile, long firstPts) throws IOException {
        SetPTS set = new SetPTS(firstPts);
        set.fix(tsfile);
    };

    // take testdata 02.ts from https://www.dropbox.com/s/g37w70lwydyd9zc/02.ts
    // originally 02.ts converted from https://www.dropbox.com/s/xb6m7yptp90z5m9/2.mp4
    // #ffmpeg -i 2.mp4 -acodec copy -vcodec copy -bsf h264_mp4toannexb 02.ts
    // ffmpeg version 2.3.3
    @Test
    public void testSetPts() throws Exception {
<<<<<<< HEAD
        File largeTs = new File("02.ts");
        File noSoundTs = new File("02nosound.ts");
=======
        File largeTs = new File("/Users/vitvitskyy/Desktop/02.ts");
        File noSoundTs = new File("/Users/vitvitskyy/Desktop/02nosound.ts");
>>>>>>> master
        doCopyFile(largeTs, noSoundTs, true);
        setFirstPts(noSoundTs, 609483);
    }

    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size) {
                count = size - pos;
                pos += output.transferFrom(input, pos, count);
            }
        } finally {
            output.close();
            fos.close();
            input.close();
            fis.close();
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }
}
