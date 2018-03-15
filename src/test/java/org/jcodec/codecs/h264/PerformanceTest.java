package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.conformance.ConformanceTest;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.platform.Platform;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;
import static org.jcodec.codecs.h264.conformance.ConformanceTest.duplicateBuffers;
import static org.jcodec.codecs.h264.conformance.ConformanceTest.extractNALUnits;
import static org.jcodec.codecs.h264.conformance.ConformanceTest.readFile;
import static org.jcodec.common.StringUtils.zeroPad3;

public class PerformanceTest {

    @Test
    public void testNoContainer() throws IOException {
        new ConformanceTest().testNoContainer();
        String dir = "src/test/resources/video/seq_h264_4";

        String info = Platform.stringFromBytes(readFile(dir + "/info.txt").array());
        int width = parseInt(info.split(" ")[0]);
        int height = parseInt(info.split(" ")[1]);
        int frameCount = parseInt(info.split(" ")[2]);

        byte[][] picData = Picture.create(width, height, ColorSpace.YUV420J).getData();

        H264Decoder decoder = new H264Decoder();
        decoder.addSps(singletonList(readFile(dir + "/sps")));
        decoder.addPps(singletonList(readFile(dir + "/pps")));

        List<List<ByteBuffer>> frames = new ArrayList<List<ByteBuffer>>();

        for (int fn = 0; fn < frameCount; fn++) {
            ByteBuffer buf = readFile(dir + "/" + zeroPad3(fn));
            frames.add(extractNALUnits(buf));
        }

        for (int i = 1, warmUpIterations = 30; i <= warmUpIterations; i++) {
            System.out.println("warming up " + i + "/" + warmUpIterations);

            for (int fn = 0; fn < frameCount; fn++)
                decoder.decodeFrameFromNals(duplicateBuffers(frames.get(fn)), picData);
        }

        int fpss = 0;
        int iterations = 15;

        for (int i = 1; i <= iterations; i++) {
            System.out.println("benchmarking " + i + "/" + iterations);
            long t = System.currentTimeMillis();

            for (int fn = 0; fn < frameCount; fn++)
                decoder.decodeFrameFromNals(duplicateBuffers(frames.get(fn)), picData);

            t = System.currentTimeMillis() - t;
            long fps = frames.size() * 1000 / t;
            System.out.println(fps + " fps");
            fpss += fps;
        }

        System.out.println("\naverage: " + (fpss / iterations) + " fps");
    }

}
