package org.jcodec.testing;

import static org.jcodec.common.JCodecUtil.getAsIntArray;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VerifyTool {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Syntax: <error folder location>");
            return;
        }
        new VerifyTool().doIt(args[0]);
    }

    private void doIt(String location) throws IOException {
        File[] h264 = new File(location).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".264");
            }
        });
        for (File coded : h264) {
            File ref = new File(coded.getParentFile(), coded.getName().replaceAll(".264$", "_dec.yuv"));
            if (coded.exists() && ref.exists()) {
                try {
                    if (test(coded, ref)) {
                        System.out.println(coded.getAbsolutePath() + " -- FIXED");
                        coded.delete();
                        ref.delete();
                    } else {
                        System.out.println(coded.getAbsolutePath() + " -- NOT FIXED!!!!");
                    }
                } catch (Throwable t) {
                    System.out.println(coded.getAbsolutePath() + " -- ERROR: " + t.getMessage());
                }
            }
        }
    }

    private boolean test(File coded, File ref) throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(coded));
        Picture buf = Picture.create(1920, 1088, ColorSpace.YUV420);
        H264Decoder dec = new H264Decoder();
        Packet nextFrame;
        ByteBuffer _yuv = NIOUtils.fetchFrom(ref);
        while ((nextFrame = es.nextFrame()) != null) {
            Picture out = dec.decodeFrame(nextFrame.getData(), buf.getData()).cropped();
            Picture pic = out.createCompatible();
            pic.copyFrom(out);
            int lumaSize = pic.getWidth() * pic.getHeight();
            int crSize = lumaSize >> 2;
            int cbSize = lumaSize >> 2;

            ByteBuffer yuv = NIOUtils.read(_yuv, lumaSize + crSize + cbSize);

            if (!Arrays.equals(getAsIntArray(yuv, lumaSize), pic.getPlaneData(0)))
                return false;
            if (!Arrays.equals(getAsIntArray(yuv, crSize), pic.getPlaneData(1)))
                return false;
            if (!Arrays.equals(getAsIntArray(yuv, cbSize), pic.getPlaneData(2)))
                return false;
        }
        return true;
    }
}
