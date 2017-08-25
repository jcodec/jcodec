package org.jcodec.codecs.prores;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Assert;
import org.junit.Test;

public class ProresDecoderTest {
    public static final String PRORES_PATH = "src/test/resources/test.prores.mov";
    public static final String YUV_FULL_1x_PATH = "src/test/resources/test.prores.d1.yuv";
    public static final String YUV_FULL_1x_10_PATH = "src/test/resources/test.prores.yuv";
    public static final String YUV_FULL_2x_PATH = "src/test/resources/test.prores.d2.yuv";
    public static final String YUV_FULL_4x_PATH = "src/test/resources/test.prores.d4.yuv";
    public static final String YUV_FULL_8x_PATH = "src/test/resources/test.prores.d8.yuv";

    @Test
    public void testFullSize2Frames() throws IOException {
        testOneMovie(PRORES_PATH, YUV_FULL_1x_PATH, new ProresDecoder(), false);
    }
    
    @Test
    public void testFullSize2Frames10Bit() throws IOException {
        testOneMovie(PRORES_PATH, YUV_FULL_1x_10_PATH, new ProresDecoder(), true);
    }
    
    @Test
    public void testHalfSize2Frames() throws IOException {
        testOneMovie(PRORES_PATH, YUV_FULL_2x_PATH, new ProresToThumb4x4(), false);
    }
    
    @Test
    public void testQuarterSize2Frames() throws IOException {
        testOneMovie(PRORES_PATH, YUV_FULL_4x_PATH, new ProresToThumb2x2(), false);
    }
    
    @Test
    public void testQuaverSize2Frames() throws IOException {
        testOneMovie(PRORES_PATH, YUV_FULL_8x_PATH, new ProresToThumb(), false);
    }
    
    public void testOneMovie(String proresPath, String yuvPath, ProresDecoder decoder, boolean hibd) throws IOException {
        SeekableByteChannel inCh = null;
        SeekableByteChannel refCh = null;
        try {
            inCh = NIOUtils.readableChannel(new File(proresPath));
            refCh = NIOUtils.readableChannel(new File(yuvPath));
            MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(inCh);
            DemuxerTrack videoTrack = demuxer.getVideoTrack();
            VideoCodecMeta meta = videoTrack.getMeta().getCodecMeta().video();
            Size size = meta.getSize();
            Picture buffer = null;
            if (hibd) {
                buffer = Picture.createCroppedHiBD((size.getWidth() + 15) & ~0xf, (size.getHeight() + 15) & ~0xf, 2,
                        ColorSpace.YUV422, null);
            } else {
                buffer = Picture.createCropped((size.getWidth() + 15) & ~0xf, (size.getHeight() + 15) & ~0xf,
                        ColorSpace.YUV422, null);
            }
            for (int i = 0; i < 2; i++) {
                Packet nextFrame = videoTrack.nextFrame();
                Picture decoded;
                if (hibd) {
                    decoded = decoder.decodeFrameHiBD(nextFrame.getData(), buffer.getData(), buffer.getLowBits());
                } else {
                    decoded = decoder.decodeFrame(nextFrame.getData(), buffer.getData());
                }
                Assert.assertTrue(compare(refCh, decoded));
            }
        } finally {
            if (inCh != null)
                inCh.close();
            if (refCh != null)
                refCh.close();
        }
    }

    private boolean compare(SeekableByteChannel refCh, Picture decoded) throws IOException {
        int imgSize = (((decoded.getCroppedWidth() * decoded.getCroppedHeight() * decoded.getColor().bitsPerPixel + 7) >> 3)) << (decoded
                .isHiBD() ? 1 : 0);
        ByteBuffer raw = ByteBuffer.allocate(imgSize);
        raw.order(ByteOrder.LITTLE_ENDIAN);
        if (refCh.read(raw) != imgSize)
            throw new IOException("Expected " + imgSize + " bytes.");
        raw.flip();
        for (int comp = 0; comp < 3; comp++) {
            int width = decoded.getCroppedWidth() >> decoded.getColor().compWidth[comp];
            int height = decoded.getCroppedHeight() >> decoded.getColor().compHeight[comp];
            int stride = decoded.getWidth() >> decoded.getColor().compWidth[comp];
            for (int r = 0, ind = 0; r < height; r++) {
                for (int c = 0; c < width; c++, ind++) {
                    int refP = getRefPixel(raw, decoded.getLowBitsNum());
                    int decP = getDecPixel(decoded, comp, ind);
                    if (Math.abs(refP - decP) > 2) {
                        System.out.println("ref[" + c + ", " + r + "] != dec[" + c + ", " + r + "] (" + refP + ","
                                + decP + ")");
                        return false;
                    }
                }
                ind += stride - width;
            }
        }
        return true;
    }

    private int getDecPixel(Picture decoded, int comp, int ind) {
        int pix = decoded.getPlaneData(comp)[ind] + 128;
        return decoded.isHiBD() ? (pix << 2) + decoded.getLowBits()[comp][ind] : pix;
    }

    private int getRefPixel(ByteBuffer raw, int hiBits) {
        return hiBits > 0 ? (raw.getShort() & 0xffff) : (raw.get() & 0xff);
    }
}
