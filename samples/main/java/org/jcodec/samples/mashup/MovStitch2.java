package org.jcodec.samples.mashup;

import static org.jcodec.common.NIOUtils.readableFileChannel;
import static org.jcodec.common.NIOUtils.writableFileChannel;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4DemuxerException;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.AVC1Box;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox.MyFactory;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class MovStitch2 {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Syntax: <in1.mov> <in2.mov> <out.mov>");
            return;
        }

        File in1 = new File(args[0]);
        File in2 = new File(args[1]);
        File out = new File(args[2]);

        changePPS(in1, in2, out);
    }

    public static void changePPS(File in1, File in2, File out) throws IOException, MP4DemuxerException,
            FileNotFoundException {
        MP4Muxer muxer = new MP4Muxer(writableFileChannel(out), Brand.MOV);

        MP4Demuxer demuxer1 = new MP4Demuxer(readableFileChannel(in1));
        AbstractMP4DemuxerTrack vt1 = demuxer1.getVideoTrack();
        MP4Demuxer demuxer2 = new MP4Demuxer(readableFileChannel(in2));
        AbstractMP4DemuxerTrack vt2 = demuxer2.getVideoTrack();
        checkCompatible(vt1, vt2);

        FramesMP4MuxerTrack outTrack = muxer.addTrackForCompressed(VIDEO, (int) vt1.getTimescale());
        outTrack.addSampleEntry(vt1.getSampleEntries()[0]);
        for (int i = 0; i < vt1.getFrameCount(); i++) {
            outTrack.addFrame((MP4Packet) vt1.nextFrame());
        }

        AvcCBox avcC = doSampleEntry(vt2, outTrack);

        SliceHeaderReader shr = new SliceHeaderReader();
        SeqParameterSet sps = SeqParameterSet.read(avcC.getSpsList().get(0).duplicate());
        PictureParameterSet pps = PictureParameterSet.read(avcC.getPpsList().get(0).duplicate());
        SliceHeaderWriter shw = new SliceHeaderWriter();

        for (int i = 0; i < vt2.getFrameCount(); i++) {
            MP4Packet packet = (MP4Packet) vt2.nextFrame();
            ByteBuffer frm = doFrame(packet.getData(), shr, shw, sps, pps);
            outTrack.addFrame(new MP4Packet(packet, frm));
        }

        AvcCBox first = Box.findFirst(vt1.getSampleEntries()[0], AvcCBox.class, AvcCBox.fourcc());
        AvcCBox second = Box.findFirst(vt2.getSampleEntries()[0], AvcCBox.class, AvcCBox.fourcc());
        first.getSpsList().addAll(second.getSpsList());
        first.getPpsList().addAll(second.getPpsList());

        muxer.writeHeader();
    }

    private static void checkCompatible(AbstractMP4DemuxerTrack vt1, AbstractMP4DemuxerTrack vt2) {
        VideoSampleEntry se1 = (VideoSampleEntry) vt1.getSampleEntries()[0];
        VideoSampleEntry se2 = (VideoSampleEntry) vt2.getSampleEntries()[0];

        Assert.assertEquals(vt1.getSampleEntries().length, 1);
        Assert.assertEquals(vt2.getSampleEntries().length, 1);
        Assert.assertEquals(se1.getFourcc(), "avc1");
        Assert.assertEquals(se2.getFourcc(), "avc1");
        Assert.assertEquals(se1.getWidth(), se2.getWidth());
        Assert.assertEquals(se1.getHeight(), se2.getHeight());
    }

    public static ByteBuffer doFrame(ByteBuffer data, SliceHeaderReader shr, SliceHeaderWriter shw,
            SeqParameterSet sps, PictureParameterSet pps) throws IOException {
        ByteBuffer result = ByteBuffer.allocate(data.remaining() + 24);
        while (data.remaining() > 0) {
            int len = data.getInt();

            NALUnit nu = NALUnit.read(data);
            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {

                ByteBuffer savePoint = result.duplicate();
                result.getInt();
                copyNU(shr, shw, nu, data, result, sps, pps);

                savePoint.putInt(savePoint.remaining() - result.remaining() - 4);
            } else {
                result.putInt(len);
                nu.write(result);
                NIOUtils.write(result, data);
            }
        }

        result.flip();

        return result;
    }

    public static void copyNU(SliceHeaderReader shr, SliceHeaderWriter shw, NALUnit nu, ByteBuffer is, ByteBuffer os,
            SeqParameterSet sps, PictureParameterSet pps) {
        BitReader reader = new BitReader(is);
        BitWriter writer = new BitWriter(os);

        SliceHeader sh = shr.readPart1(reader);
        shr.readPart2(sh, nu, sps, pps, reader);
        sh.pic_parameter_set_id = 1;

        nu.write(os);
        shw.write(sh, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, writer);

        copyCABAC(writer, reader);
    }

    private static AvcCBox doSampleEntry(AbstractMP4DemuxerTrack videoTrack, FramesMP4MuxerTrack outTrack) {
        SampleEntry se = videoTrack.getSampleEntries()[0];

        AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) se);
        AvcCBox old = H264Utils.parseAVCC((VideoSampleEntry) se);
        
        for (int i = 0; i < avcC.getPpsList().size(); i++) {
            avcC.getPpsList().set(i, updatePps(avcC.getPpsList().get(i)));
        }

        for (int i = 0; i < avcC.getSpsList().size(); i++) {
            avcC.getSpsList().set(i, updateSps(avcC.getSpsList().get(i)));
        }

        return old;
    }

    private static void copyCABAC(BitWriter w, BitReader r) {
        long bp = r.curBit();
        long rem = r.readNBit(8 - (int) bp);
        Assert.assertEquals((1 << (8 - bp)) - 1, rem);

        if (w.curBit() != 0)
            w.writeNBit(0xff, 8 - w.curBit());
        int b;
        while ((b = r.readNBit(8)) != -1)
            w.writeNBit(b, 8);
    }

    static ByteBuffer updateSps(ByteBuffer bb) {
        SeqParameterSet sps = SeqParameterSet.read(bb);
        sps.seq_parameter_set_id = 1;
        ByteBuffer out = ByteBuffer.allocate(bb.capacity() + 10);
        sps.write(out);
        out.flip();
        return out;
    }

    static ByteBuffer updatePps(ByteBuffer bb) {
        PictureParameterSet pps = PictureParameterSet.read(bb);
        Assert.assertTrue(pps.entropy_coding_mode_flag);
        pps.seq_parameter_set_id = 1;
        pps.pic_parameter_set_id = 1;
        ByteBuffer out = ByteBuffer.allocate(bb.capacity() + 10);
        pps.write(out);
        out.flip();
        return out;
    }
}
