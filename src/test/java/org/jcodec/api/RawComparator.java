package org.jcodec.api;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * Used to compare reference YUV file to the decoded frames
 * 
 * @author Stanislav Vitvitskyy
 *
 */
public class RawComparator {

    private File file;
    private int fn;
    private int tolerance;

    public RawComparator(String name, int tolerance) {
        this.file = new File(name);
        this.tolerance = tolerance;
    }

    public boolean nextFrame(Picture decoded) throws IOException {
        Picture raw = readRaw(file, fn++, decoded.getCroppedWidth(), decoded.getCroppedHeight());
        decoded = decoded.cropped();

        return assertByteArrayApproximatelyEquals(raw.getPlaneData(0), decoded.getPlaneData(0), tolerance)
                && assertByteArrayApproximatelyEquals(raw.getPlaneData(1), decoded.getPlaneData(1), tolerance)
                && assertByteArrayApproximatelyEquals(raw.getPlaneData(2), decoded.getPlaneData(2), tolerance);
    }

    private boolean assertByteArrayApproximatelyEquals(byte[] rand, byte[] newRand, int threash) {
        int maxDiff = 0;
        for (int i = 0; i < rand.length; i++) {
            int diff = Math.abs(rand[i] - newRand[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }
        return maxDiff < threash;
    }

    private Picture readRaw(File file, int fn, int width, int height) throws IOException {
        int fs = width * height * 3 / 2;
        ByteBuffer rawBuffer = NIOUtils.fetchFromFileOL(file, fs * fn, fs);
        Picture result = Picture.create(width, height, ColorSpace.YUV420);

        for (int i = 0; i < width * height; i++) {
            result.getPlaneData(0)[i] = (byte) ((rawBuffer.get() & 0xff) - 128);
        }

        for (int comp = 1; comp < 3; comp++) {
            for (int i = 0; i < ((width * height) >> 2); i++) {
                result.getPlaneData(comp)[i] = (byte) ((rawBuffer.get() & 0xff) - 128);
            }
        }

        return result;
    }
}
