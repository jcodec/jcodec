package org.jcodec.samples.gen;

import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.codecs.raw.V210Encoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This code generates two dark gradients 8bit and 10 bit quantization and saves
 * it into v210 mov
 * 
 * @author The JCodec project
 * 
 */
public class GradGen {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("<out mov> <n frames>");
            return;
        }
        int width = 640;
        int height = 480;
        Picture pic = Picture.create(width, height, ColorSpace.YUV422_10);
        Arrays.fill(pic.getPlaneData(1), 512);
        Arrays.fill(pic.getPlaneData(2), 512);

        drawGrad(pic.getPlaneData(0), new Size(pic.getWidth(), pic.getHeight()));

        V210Encoder encoder = new V210Encoder();
        MP4Muxer muxer = new MP4Muxer(writableFileChannel(new File(args[0])));

        ByteBuffer out = ByteBuffer.allocate(width * height * 10);
        ByteBuffer frame = encoder.encodeFrame(out, pic);

        FramesMP4MuxerTrack videoTrack = muxer.addVideoTrack("v210", new Size(width, height), "jcodec", 24000);

        for (int i = 0; i < Integer.parseInt(args[1]); i++) {
            videoTrack.addFrame(new MP4Packet(frame, i * 1001, 24000, 1001, i, true, null, i * 1001, 0));
        }
        muxer.writeHeader();
    }

    private static void drawGrad(int[] y, Size ySize) {
        int blockX = ySize.getWidth() / 10;
        int blockY = ySize.getHeight() / 7;

        fillGrad(y, ySize.getWidth(), blockX, blockY, 9 * blockX, 3 * blockY, 0.2, 0.1, 8);
        fillGrad(y, ySize.getWidth(), blockX, 4 * blockY, 9 * blockX, 6 * blockY, 0.2, 0.1, 10);
    }

    private static void fillGrad(int[] y, int stride, int left, int top, int right, int bottom, double from, double to,
            int quant) {
        int step = stride + left - right;
        int off = top * stride + left;
        for (int j = top; j < bottom; j++) {
            for (int i = left; i < right; i++) {
                y[off++] = colr(((double) i - left) / (right - left), ((double) j - top) / (bottom - top), from, to,
                        quant);
            }
            off += step;
        }
    }

    private static int colr(double i, double j, double from, double to, int quant) {
        int val = ((int) ((1 << quant) * (from + (to - from) * i))) << (10 - quant);
        return val;
    }
}