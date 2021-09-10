package org.jcodec.samples.vp8;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;

import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.api.transcode.Sink;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.VideoFrameWithPacket;
import org.jcodec.codecs.raw.RAWVideoDecoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The class contains a simple transcoder which demos the VP8 encoder's
 * capabilities. It is offering the same basic functionality you would get from
 * libvpx's official encoder. Parameters:
 * <ol>
 * <li>The input yuv video file to transform to VP8</li>
 * <li>The output webm video file to transform the yuv video into</li>
 * <li>The horizontal resolution of the input video</li>
 * <li>The vertical resolution of the input video</li>
 * <li>The number of frames/s we need to encode in the webm file</li>
 * </ol>
 * 
 * @author The JCodec project
 * 
 */
public class RawToWebM {
    public static void main(String[] args) throws Exception {
        long before = System.currentTimeMillis();
        int timestamp = 0;
        Rational fps = Rational.R(Integer.parseInt(args[4]), 1);
        final int cols = Integer.parseInt(args[2]), rows = Integer.parseInt(args[3]);
        RAWVideoDecoder rvd = new RAWVideoDecoder(cols, rows);
        SeekableByteChannel targetChannel = NIOUtils.writableFileChannel(args[1]);
        Sink sink = SinkImpl.createWithStream(targetChannel, Format.MKV, Codec.VP8, null);
        sink.setCodecOpts(Collections.singletonMap(Codec.VP8.name().toLowerCase(), "qp:" + 20 + "," + "scmode:" + 1));
        sink.init(false, false);
        FileChannel fc = FileChannel.open(new File(args[0]).toPath());
        byte[][] frameData = new byte[3][];
        frameData[0] = new byte[cols * rows];
        frameData[1] = new byte[cols * rows / 4];
        frameData[2] = new byte[cols * rows / 4];
        final int completeLen = frameData[0].length + frameData[1].length * 2;
        int readCount;
        ByteBuffer bb = ByteBuffer.allocate(completeLen);
        int count = 0;
        do {
            readCount = fc.read(bb);
            bb.flip();
            LoanerPicture toEncode = new LoanerPicture(rvd.decodeFrame(bb.duplicate(), frameData), 0);
            Packet pkt = Packet.createPacket(null, timestamp, fps.getNum(), fps.getDen(), count, FrameType.KEY, null);
            sink.outputVideoFrame(new VideoFrameWithPacket(pkt, toEncode));
            bb.rewind();
            timestamp += fps.getDen();
            count++;
        } while (readCount > 0);
        sink.finish();
        System.out.println("Number of frames encoded: " + count);
        System.out.println("Complete encoding duration: " + (System.currentTimeMillis() - before) + "ms");
    }
}
