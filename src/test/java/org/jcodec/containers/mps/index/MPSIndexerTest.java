package org.jcodec.containers.mps.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.MPEGHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureCodingExtension;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceExtension;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.common.ByteBufferSeekableByteChannel;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.containers.mps.index.MPSIndex.MPSStreamIndex;
import org.junit.Assert;
import org.junit.Test;

public class MPSIndexerTest {

    byte[] syncMarker = { 0, 0, 1 };

    byte[] mpegStreamParams = flatten(new byte[][] {
//@formatter:off
      syncMarker, {(byte)0xb3}, toHex(new SequenceHeader(1920, 1080, 1, 1, 512*1024, 0, 0, null, null)), 
      syncMarker, {(byte)0xb5}, toHex(new SequenceExtension(100, 1, 1, 0, 0, 0, 0, 0, 0, 0)),
      syncMarker, {(byte)0xb8}, toHex(new GOPHeader(new TapeTimecode((short)1, (byte)0, (byte)0, (byte)10, false), false, false)) 
  //@formatter:on
    });

    byte[] mpegSlices = flatten(new byte[][] {
//@formatter:off
      syncMarker, {0x1, 0, 0, 0},
      syncMarker, {0x2, 0, 0, 0},
      syncMarker, {0x3, 0, 0, 0},
  //@formatter:on
    });

    byte[] mpegFrame(int tempRef, int frameType) {
        return flatten(new byte[][] {
//@formatter:off
      syncMarker, {0}, toHex(new PictureHeader(tempRef, frameType, 0, 0, 0, 0, 0)),
      syncMarker, {(byte)0xb5}, toHex(new PictureCodingExtension()),
      mpegSlices
  //@formatter:on
        });
    }

    byte[] iFrame = flatten(new byte[][] { mpegStreamParams, mpegFrame(0, 1) });

    private byte[] flatten(byte[][] data) {
        int total = 0;
        for (int i = 0; i < data.length; i++) {
            total += data[i].length;
        }
        byte[] result = new byte[total];
        for (int i = 0, off = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++, off++) {
                result[off] = data[i][j];
            }
        }
        return result;
    }

    public byte[] ts(long ts) {
//@formatter:off
        // a3 a2 a1 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 00 00 b7 b6 b5 b4 b3 b2 b1 b0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 c7 c6 c5 c4 c3 c2 c1 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 d7 d6 d5 d4 d3 d2 d1 d0 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 e7 e6 e5 e4 e3 e2 e1
//@formatter:on        
        return new byte[] { (byte) ((ts >> 29) << 1), (byte) (ts >> 22), (byte) ((ts >> 15) << 1), (byte) (ts >> 7),
                (byte) ((ts & 0x7f) << 1) };
    }

    public byte[] sub(byte[] arr, int off, int length) {
        return Arrays.copyOfRange(arr, off, Math.min(arr.length, off + length));
    }

    byte[] pes(int streamId, int pts, int dts, int payloadLen, int off, byte[] es, boolean zeroLen) {
        byte[] sub = sub(es, off, payloadLen);
        int pesLen = zeroLen ? 0 : sub.length + 13;
        return flatten(new byte[][] {
//@formatter:off
                { 0x00, 0x00, 0x01, (byte) streamId, (byte) (pesLen >> 8), (byte) (pesLen & 0xff), (byte) 0x80, (byte) 0xc0, 10 },
                ts(pts),
                ts(dts),
                sub 
        });
        //@formatter:on
    }

    byte[] pes(int streamId, int payloadLen, int off, byte[] es, boolean zeroLen) {
        byte[] sub = sub(es, off, payloadLen);
        int pesLen = zeroLen ? 0 : sub.length + 3;
        return flatten(new byte[][] {
//@formatter:off
                { 0x00, 0x00, 0x01, (byte) streamId, (byte) (pesLen >> 8), (byte) (pesLen & 0xff), (byte) 0x80, (byte) 0x00, 0 },
                sub 
        });
        //@formatter:on
    }

    private void printHex(byte[] mpegPS) {
        for (int i = 0; i < mpegPS.length; i += 32) {
            for (int j = i; j < Math.min(mpegPS.length, i + 32); j++) {
                System.out.print(String.format("%02x", mpegPS[j]) + " ");
            }
            System.out.println();
        }
    }

    private <T extends MPEGHeader> byte[] toHex(T struct) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        struct.write(bb);
        bb.flip();
        return NIOUtils.toArray(bb);
    }

    @Test
    public void testSingleVideoLengthPresentPtsPresent() throws IOException {
        byte[][] mpegFrames = {
//@formatter:off
            iFrame, 
            mpegFrame(5, 2),
            mpegFrame(2, 3), 
            mpegFrame(3, 3), 
            mpegFrame(4, 3),
//@formatter:on
        };
        byte[] mpegES = flatten(mpegFrames);

        int pesLen = mpegFrames[1].length / 3;
        byte[][] peses = {
//@formatter:off
            pes(0xe0, pesLen, 0, mpegES, false),
            pes(0xe0, pesLen, pesLen, mpegES, false),
            pes(0xe0, 10000, 9995, pesLen, pesLen*2, mpegES, false),
            pes(0xe0, pesLen, pesLen*3, mpegES, false),
            pes(0xe0, pesLen, pesLen*4, mpegES, false),
            pes(0xe0, 13003, 12998, pesLen, pesLen*5, mpegES, false),
            pes(0xe0, pesLen, pesLen*6, mpegES, false),
            pes(0xe0, pesLen, pesLen*7, mpegES, false),
            pes(0xe0, pesLen, pesLen*8, mpegES, false),
            pes(0xe0, 16006, 16001, pesLen, pesLen*9, mpegES, false),
            pes(0xe0, pesLen, pesLen*10, mpegES, false),
            pes(0xe0, pesLen, pesLen*11, mpegES, false),
            pes(0xe0, 19009, 19004, pesLen, pesLen*12, mpegES, false),
            pes(0xe0, pesLen, pesLen*13, mpegES, false),
            pes(0xe0, pesLen, pesLen*14, mpegES, false),
            pes(0xe0, 21012, 21007, pesLen, pesLen*15, mpegES, false),
            pes(0xe0, pesLen, pesLen*16, mpegES, false),
            pes(0xe0, pesLen, pesLen*17, mpegES, false),
            pes(0xe0, pesLen, pesLen*18, mpegES, false),
//@formatter:on
        };
        byte[] mpegPS = flatten(peses);

        printHex(mpegPS);

        MPSIndexer mpsIndexer = new MPSIndexer();
        mpsIndexer.index(new ByteBufferSeekableByteChannel(ByteBuffer.wrap(mpegPS)), null);
        MPSIndex index = mpsIndexer.serialize();
        long[] pesTokens = index.getPesTokens();
        Assert.assertEquals(peses.length, pesTokens.length);
        for (int i = 0; i < pesTokens.length; i++) {
            Assert.assertEquals(i == pesTokens.length - 1 ? (mpegES.length % pesLen) : pesLen,
                    MPSIndex.payLoadSize(pesTokens[i]));
            Assert.assertEquals(peses[i].length, MPSIndex.pesLen(pesTokens[i]));
        }
        MPSStreamIndex[] streams = index.getStreams();
        Assert.assertEquals(1, streams.length);
        MPSStreamIndex stream = streams[0];
        Assert.assertArrayEquals(new int[] { 10000, 13003, 16006, 19009, 21012 }, stream.getFpts());
        Assert.assertArrayEquals(new int[] { mpegFrames[0].length, mpegFrames[1].length, mpegFrames[2].length,
                mpegFrames[3].length, mpegFrames[4].length }, stream.fsizes);
        Assert.assertArrayEquals(new int[] { 0 }, stream.sync);
    }

    @Test
    public void testInterleavedLengthPresentPtsPresent() throws IOException {
        byte[] EMPTY = { 0, 0 };
        byte[][] mpegFrames = {
//@formatter:off
            iFrame, 
            mpegFrame(5, 2),
            mpegFrame(2, 3), 
            mpegFrame(3, 3), 
            mpegFrame(4, 3),
//@formatter:on
        };
        byte[] mpegES = flatten(mpegFrames);

        int pesLen = mpegFrames[1].length / 3;
        byte[][] peses = {
//@formatter:off
            pes(0xe0, pesLen, 0, mpegES, false),
            pes(0xe0, pesLen, pesLen, mpegES, false),
            pes(0xbd, 10100, 10095, 2, 0, EMPTY, false),
            pes(0xe0, 10000, 9995, pesLen, pesLen*2, mpegES, false),
            pes(0xe0, pesLen, pesLen*3, mpegES, false),
            pes(0xbd, 16100, 16095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*4, mpegES, false),
            pes(0xe0, 13003, 12998, pesLen, pesLen*5, mpegES, false),
            pes(0xbd, 22100, 22095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*6, mpegES, false),
            pes(0xe0, pesLen, pesLen*7, mpegES, false),
            pes(0xbd, 28100, 28095,2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*8, mpegES, false),
            pes(0xe0, 16006, 16001, pesLen, pesLen*9, mpegES, false),
            pes(0xbd, 34100, 34095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*10, mpegES, false),
            pes(0xe0, pesLen, pesLen*11, mpegES, false),
            pes(0xbd, 40100, 40095, 2, 0, EMPTY, false),
            pes(0xe0, 19009, 19004, pesLen, pesLen*12, mpegES, false),
            pes(0xe0, pesLen, pesLen*13, mpegES, false),
            pes(0xbd, 46100, 46095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*14, mpegES, false),
            pes(0xe0, 21012, 21007, pesLen, pesLen*15, mpegES, false),
            pes(0xbd, 52100, 52095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*16, mpegES, false),
            pes(0xe0, pesLen, pesLen*17, mpegES, false),
            pes(0xbd, 58100, 58095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*18, mpegES, false),
//@formatter:on
        };
        byte[] mpegPS = flatten(peses);

        printHex(mpegPS);

        MPSIndexer mpsIndexer = new MPSIndexer();
        mpsIndexer.index(new ByteBufferSeekableByteChannel(ByteBuffer.wrap(mpegPS)), null);
        MPSIndex index = mpsIndexer.serialize();
        long[] pesTokens = index.getPesTokens();
        Assert.assertEquals(peses.length, pesTokens.length);
        for (int i = 0; i < pesTokens.length; i++) {
            Assert.assertEquals((i % 3) == 2 ? 2 : i == pesTokens.length - 1 ? (mpegES.length % pesLen) : pesLen,
                    MPSIndex.payLoadSize(pesTokens[i]));
            Assert.assertEquals(peses[i].length, MPSIndex.pesLen(pesTokens[i]));
        }
        MPSStreamIndex[] streams = index.getStreams();
        Assert.assertEquals(2, streams.length);
        MPSStreamIndex stream = getVideoStream(streams);
        Assert.assertArrayEquals(new int[] { 10000, 13003, 16006, 19009, 21012 }, stream.getFpts());
        Assert.assertArrayEquals(new int[] { mpegFrames[0].length, mpegFrames[1].length, mpegFrames[2].length,
                mpegFrames[3].length, mpegFrames[4].length }, stream.fsizes);
        Assert.assertArrayEquals(new int[] { 0 }, stream.sync);
    }

    @Test
    public void testInterleavedNoLengthPtsPresent() throws IOException {
        byte[] EMPTY = { 0, 0 };
        byte[][] mpegFrames = {
//@formatter:off
            iFrame, 
            mpegFrame(5, 2),
            mpegFrame(2, 3), 
            mpegFrame(3, 3), 
            mpegFrame(4, 3),
//@formatter:on
        };
        byte[] mpegES = flatten(mpegFrames);

        int pesLen = mpegFrames[1].length / 3;
        byte[][] peses = {
//@formatter:off
            pes(0xe0, pesLen, 0, mpegES, true),
            pes(0xe0, pesLen, pesLen, mpegES, true),
            pes(0xbd, 10100, 10095, 2, 0, EMPTY, false),
            pes(0xe0, 10000, 9995, pesLen, pesLen*2, mpegES, true),
            pes(0xe0, pesLen, pesLen*3, mpegES, true),
            pes(0xbd, 16100, 16095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*4, mpegES, true),
            pes(0xe0, 13003, 12998, pesLen, pesLen*5, mpegES, true),
            pes(0xbd, 22100, 22095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*6, mpegES, true),
            pes(0xe0, pesLen, pesLen*7, mpegES, true),
            pes(0xbd, 28100, 28095,2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*8, mpegES, true),
            pes(0xe0, 16006, 16001, pesLen, pesLen*9, mpegES, true),
            pes(0xbd, 34100, 34095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*10, mpegES, true),
            pes(0xe0, pesLen, pesLen*11, mpegES, true),
            pes(0xbd, 40100, 40095, 2, 0, EMPTY, false),
            pes(0xe0, 19009, 19004, pesLen, pesLen*12, mpegES, true),
            pes(0xe0, pesLen, pesLen*13, mpegES, true),
            pes(0xbd, 46100, 46095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*14, mpegES, true),
            pes(0xe0, 21012, 21007, pesLen, pesLen*15, mpegES, true),
            pes(0xbd, 52100, 52095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*16, mpegES, true),
            pes(0xe0, pesLen, pesLen*17, mpegES, true),
            pes(0xbd, 58100, 58095, 2, 0, EMPTY, false),
            pes(0xe0, pesLen, pesLen*18, mpegES, true),
//@formatter:on
        };
        byte[] mpegPS = flatten(peses);

        printHex(mpegPS);

        MPSIndexer mpsIndexer = new MPSIndexer();
        mpsIndexer.index(new ByteBufferSeekableByteChannel(ByteBuffer.wrap(mpegPS)), null);
        MPSIndex index = mpsIndexer.serialize();
        long[] pesTokens = index.getPesTokens();
        Assert.assertEquals(peses.length, pesTokens.length);
        for (int i = 0; i < pesTokens.length; i++) {
            Assert.assertEquals((i % 3) == 2 ? 2 : i == pesTokens.length - 1 ? (mpegES.length % pesLen) : pesLen,
                    MPSIndex.payLoadSize(pesTokens[i]));
            Assert.assertEquals(peses[i].length, MPSIndex.pesLen(pesTokens[i]));
        }
        MPSStreamIndex[] streams = index.getStreams();
        Assert.assertEquals(2, streams.length);
        MPSStreamIndex stream = getVideoStream(streams);
        Assert.assertArrayEquals(new int[] { 10000, 13003, 16006, 19009, 21012 }, stream.getFpts());
        Assert.assertArrayEquals(new int[] { mpegFrames[0].length, mpegFrames[1].length, mpegFrames[2].length,
                mpegFrames[3].length, mpegFrames[4].length }, stream.fsizes);
        Assert.assertArrayEquals(new int[] { 0 }, stream.sync);
    }

    @Test
    public void testInterleavedNoLengthPtsInterpolation() throws IOException {
        byte[] EMPTY = { 0, 0 };
        byte[][] mpegFrames = {
//@formatter:off
            iFrame, 
            mpegFrame(4, 2),
            mpegFrame(1, 3), 
            mpegFrame(2, 3), 
            mpegFrame(3, 3),
//@formatter:on
        };
        byte[] mpegES = flatten(mpegFrames);

        int pesLen = mpegFrames[1].length / 3;
        byte[][] peses = {
//@formatter:off
            pes(0xe0, pesLen, 0, mpegES, true),
            pes(0xe0, pesLen, pesLen, mpegES, true),
            
            pes(0xbd, 10100, 10095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*2, mpegES, true),
            pes(0xe0, pesLen, pesLen*3, mpegES, true),
            
            pes(0xbd, 16100, 16095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*4, mpegES, true),
            pes(0xe0, 22012, 22007, pesLen, pesLen*5, mpegES, true),
            
            pes(0xbd, 22100, 22095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*6, mpegES, true),
            pes(0xe0, pesLen, pesLen*7, mpegES, true),
            
            pes(0xbd, 28100, 28095,2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*8, mpegES, true),
            pes(0xe0, pesLen, pesLen*9, mpegES, true),
            
            pes(0xbd, 34100, 34095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*10, mpegES, true),
            pes(0xe0, pesLen, pesLen*11, mpegES, true),
            
            pes(0xbd, 40100, 40095, 2, 0, EMPTY, false),
            
            pes(0xe0, 16006, 16001, pesLen, pesLen*12, mpegES, true),
            pes(0xe0, pesLen, pesLen*13, mpegES, true),
            
            pes(0xbd, 46100, 46095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*14, mpegES, true),
            pes(0xe0, pesLen, pesLen*15, mpegES, true),
            
            pes(0xbd, 52100, 52095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*16, mpegES, true),
            pes(0xe0, pesLen, pesLen*17, mpegES, true),
            
            pes(0xbd, 58100, 58095, 2, 0, EMPTY, false),
            
            pes(0xe0, pesLen, pesLen*18, mpegES, true),
//@formatter:on
        };
        byte[] mpegPS = flatten(peses);

        printHex(mpegPS);

        MPSIndexer mpsIndexer = new MPSIndexer();
        mpsIndexer.index(new ByteBufferSeekableByteChannel(ByteBuffer.wrap(mpegPS)), null);
        MPSIndex index = mpsIndexer.serialize();
        long[] pesTokens = index.getPesTokens();
        Assert.assertEquals(peses.length, pesTokens.length);
        for (int i = 0; i < pesTokens.length; i++) {
            Assert.assertEquals((i % 3) == 2 ? 2 : i == pesTokens.length - 1 ? (mpegES.length % pesLen) : pesLen,
                    MPSIndex.payLoadSize(pesTokens[i]));
            Assert.assertEquals(peses[i].length, MPSIndex.pesLen(pesTokens[i]));
        }
        MPSStreamIndex[] streams = index.getStreams();
        Assert.assertEquals(2, streams.length);
        MPSStreamIndex stream = getVideoStream(streams);
        Assert.assertArrayEquals(new int[] { 10000, 22012, 13003, 16006, 19009 }, stream.getFpts());
        Assert.assertArrayEquals(new int[] { mpegFrames[0].length, mpegFrames[1].length, mpegFrames[2].length,
                mpegFrames[3].length, mpegFrames[4].length }, stream.fsizes);
        Assert.assertArrayEquals(new int[] { 0 }, stream.sync);
    }

    private MPSStreamIndex getVideoStream(MPSStreamIndex[] streams) {
        for (MPSStreamIndex stream : streams) {
            if (MPSUtils.videoStream(stream.streamId))
                return stream;
        }
        return null;
    }
}
