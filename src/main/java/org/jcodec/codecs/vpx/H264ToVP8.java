package org.jcodec.codecs.vpx;

import static org.jcodec.codecs.h264.H264Utils.readPPSAsArray;
import static org.jcodec.codecs.h264.H264Utils.readSPSAsArray;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.jcodec.codecs.h264.H264ES;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.codecs.h264.tweak.H264Parser;
import org.jcodec.codecs.transcode.H264ToVP8Adapter;
import org.jcodec.codecs.vpx.tweak.VP8Serializer;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Test;

public class H264ToVP8 {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Syntax: [--nal] <in file> <out file>");
            System.out.println("Where: \n" + "\t--nal:\tInput is a NAL unit stream ( MP4 file by default )\n"
                    + "\tin file:\tInput file (mov or mp4) or '-' for stdin ( --nal option is assumed in this case)\n"
                    + "\tout file:\t Output file (ivf) or '-' for IVF on stdout\n");
            return;
        }
        WritableByteChannel outCh;
        int idx = 0;
        boolean nal = false;
        if ("--nal".equals(args[idx])) {
            nal = true;
            idx++;
        }

        String in = args[idx];
        String out = args[idx + 1];

        if ("-".equals(out))
            outCh = Channels.newChannel(System.out);
        else
            outCh = NIOUtils.writableFileChannel(new File(out));

        if ("-".equals(in)) {
            readFramesNAL(Channels.newChannel(System.in), outCh);
        } else {
            FileChannelWrapper inCh = NIOUtils.readableFileChannel(new File(in));
            if (nal)
                readFramesNAL(inCh, outCh);
            else
                readFramesMP4(inCh, outCh);
            NIOUtils.closeQuietly(inCh);
        }

        NIOUtils.closeQuietly(outCh);
    }

    private static void readFramesNAL(ReadableByteChannel inCh, WritableByteChannel outCh) throws IOException {
        H264ES h264es = new H264ES(inCh);
        ByteBuffer buf = ByteBuffer.allocate(640 * 640 * 2), bufVP8 = ByteBuffer.allocate(640 * 640 * 2);
        Packet frame = h264es.nextFrame(buf);
        Picture predH264[] = { null };
        Picture predVP8 = null;
        SeqParameterSet sps = h264es.getSps()[0];
        int mbWidth = sps.pic_width_in_mbs_minus1 + 1;
        int mbHeight = H264Utils.getPicHeightInMbs(sps);
        IVFMuxer.writeIVFHeader(outCh, mbWidth << 4, mbHeight << 4, 0, 25);
        for (int i = 0; frame != null;) {

            List<ByteBuffer> nals = H264Utils.splitFrame(frame.getData());
            if (nals.size() == 0)
                continue;

            predVP8 = produceFrame(h264es.getSps(), h264es.getPps(), bufVP8, mbWidth, mbHeight, outCh, predH264,
                    predVP8, nals, i);
            i++;

            buf.clear();
            frame = h264es.nextFrame(buf);
        }
    }

    private static void readFramesMP4(SeekableByteChannel inCh, WritableByteChannel outCh) throws IOException,
            FileNotFoundException {
        MP4Demuxer demuxer = new MP4Demuxer(inCh);
        AbstractMP4DemuxerTrack vt = demuxer.getVideoTrack();

        AvcCBox avcc = H264Utils.parseAVCC((VideoSampleEntry) vt.getSampleEntries()[0]);
        ByteBuffer pps = avcc.getPpsList().get(0);
        PictureParameterSet ppsR = H264Utils.readPPS(pps);
        SeqParameterSet spsR = H264Utils.readSPS(avcc.getSpsList().get(0));
        Size picSize = H264Utils.getPicSize(spsR);

        Packet nextFrame;
        ByteBuffer buf = ByteBuffer.allocate(picSize.getWidth() * picSize.getHeight() * 2);
        SeqParameterSet sps = H264Utils.readSPS(avcc.getSpsList().get(0));
        PictureParameterSet _pps = H264Utils.readPPS(pps);
        boolean tr8x8 = _pps.extended == null ? false : _pps.extended.transform_8x8_mode_flag;
        int mbWidth = sps.pic_width_in_mbs_minus1 + 1;
        int mbHeight = H264Utils.getPicHeightInMbs(sps);

        IVFMuxer.writeIVFHeader(outCh, mbWidth << 4, mbHeight << 4, (int) vt.getFrameCount(),
                (int) Math.round(vt.getFrameCount() / vt.getDuration().scalar()));

        long start = System.currentTimeMillis();
        Picture predH264[] = { null };
        Picture predVP8 = null;
        // vt.gotoFrame(31);
        for (int i = 0/* 31 */; (nextFrame = vt.nextFrame()) != null/*
                                                                     * && i <=
                                                                     * 35
                                                                     */;) {
            List<ByteBuffer> nals = H264Utils.splitMOVPacket(nextFrame.getData(), avcc);
            if (nals.size() == 0)
                continue;
            if (nextFrame.isKeyFrame()) {
//                System.err.println("KEY: " + i);
            }
            predVP8 = produceFrame(readSPSAsArray(avcc.getSpsList()), readPPSAsArray(avcc.getPpsList()), buf, mbWidth,
                    mbHeight, outCh, predH264, predVP8, nals, i);

            if (i % 100 == 99) {
                System.err.println(i + ": " + (1000 * i / (System.currentTimeMillis() - start)) + "fps");
            }
            i++;
        }
        // System.out.println("QP stats");
        // for (int i = 0; i < 52; i++) {
        // if (counts[i] == 0)
        // counts[i] = 1;
        // System.out.print(String.format("%02d:%02d,", i, vals[i] /
        // counts[i]));
        // }
    }

    // private static int vals[] = new int[52];
    // private static int counts[] = new int[52];

    private static Picture produceFrame(SeqParameterSet[] spsa, PictureParameterSet[] ppsa, ByteBuffer buf,
            int mbWidth, int mbHeight, WritableByteChannel ch, Picture[] predH264, Picture predVP8,
            List<ByteBuffer> nals, int i) {
        try {
            buf.clear();
            boolean keyFrame = H264Utils.idrSlice(nals);
            VP8Serializer ss = new VP8Serializer(buf, mbWidth, mbHeight, keyFrame, 2);

            H264ToVP8Adapter adapter = new H264ToVP8Adapter(ss, ppsa[0].chroma_qp_index_offset,
                    ppsa[0].extended != null ? ppsa[0].extended.second_chroma_qp_index_offset
                            : ppsa[0].chroma_qp_index_offset, mbWidth, mbHeight, predH264, predVP8);
            // adapter.vals = vals;
            // adapter.counts = counts;
            new H264Parser(spsa, ppsa).parse(nals, adapter);

            predH264[0] = adapter.getRenderedH264();
            predVP8 = adapter.getRenderedVP8();

            ByteBuffer rr = ss.getResult();
            IVFMuxer.writeIVFFrame(ch, rr, i);

            // Picture pic = new Picture(480, 272, adapter.debug,
            // ColorSpace.YUV420J);
            // Arrays.fill(adapter.debug[1], 128);
            // Arrays.fill(adapter.debug[2], 128);
            // NIOUtils.closeQuietly(ch);

            // ch = NIOUtils.writableFileChannel(new File(args[1] + "." +
            // i));
            // IVFMuxer.writeIVFHeader(ch, mbWidth << 4, mbHeight << 4,
            // (int)
            // vt.getFrameCount(),
            // (int) Math.round(vt.getFrameCount() /
            // vt.getDuration().scalar()));
            // break;
        } catch (Exception e) {
            System.err.println("Broken frame " + i);
            e.printStackTrace(System.err);
        }
        return predVP8;
    }
}