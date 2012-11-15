package org.jcodec.samples.mashup;

import static org.jcodec.common.JCodecUtil.bufin;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.read.SliceHeaderReader;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.FileRAOutputStream;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.DemuxerTrack;
import org.jcodec.containers.mp4.MP4DemuxerException;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

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
        MP4Muxer muxer = new MP4Muxer(new FileRAOutputStream(out), Brand.MOV);

        MP4Demuxer demuxer1 = new MP4Demuxer(bufin(in1));
        DemuxerTrack vt1 = demuxer1.getVideoTrack();
        MP4Demuxer demuxer2 = new MP4Demuxer(bufin(in2));
        DemuxerTrack vt2 = demuxer2.getVideoTrack();
        checkCompatible(vt1, vt2);

        CompressedTrack outTrack = muxer.addTrackForCompressed(VIDEO, (int) vt1.getTimescale());
        outTrack.addSampleEntry(vt1.getSampleEntries()[0]);
        for (int i = 0; i < vt1.getFrameCount(); i++) {
            outTrack.addFrame(vt1.getFrames(1));
        }

        AvcCBox avcC = doSampleEntry(vt2, outTrack);

        SliceHeaderReader shr = new SliceHeaderReader(avcC);
        SliceHeaderWriter shw = new SliceHeaderWriter(avcC.getSPS(0), avcC.getPPS(0));

        for (int i = 0; i < vt2.getFrameCount(); i++) {
            MP4Packet packet = vt2.getFrames(1);
            Buffer frm = doFrame(packet.getData(), shr, shw);
            outTrack.addFrame(new MP4Packet(packet, frm));
        }

        AvcCBox first = Box.findFirst(vt1.getSampleEntries()[0], AvcCBox.class, AvcCBox.fourcc());
        AvcCBox second = Box.findFirst(vt2.getSampleEntries()[0], AvcCBox.class, AvcCBox.fourcc());
        first.getSpsList().addAll(second.getSpsList());
        first.getPpsList().addAll(second.getPpsList());

        muxer.writeHeader();
    }

    private static void checkCompatible(DemuxerTrack vt1, DemuxerTrack vt2) {
        VideoSampleEntry se1 = (VideoSampleEntry) vt1.getSampleEntries()[0];
        VideoSampleEntry se2 = (VideoSampleEntry) vt2.getSampleEntries()[0];

        Assert.assertEquals(vt1.getSampleEntries().length, 1);
        Assert.assertEquals(vt2.getSampleEntries().length, 1);
        Assert.assertEquals(se1.getFourcc(), "avc1");
        Assert.assertEquals(se2.getFourcc(), "avc1");
        Assert.assertEquals(se1.getWidth(), se2.getWidth());
        Assert.assertEquals(se1.getHeight(), se2.getHeight());
    }

    public static Buffer doFrame(Buffer data, SliceHeaderReader shr, SliceHeaderWriter shw) throws IOException {
        Buffer result = new Buffer(data.remaining() + 24);
        while (data.remaining() > 0) {
            int len = data.dinp().readInt();

            InputStream in = data.read(len).is();
            NALUnit nu = NALUnit.read(in);
            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {

                Buffer savePoint = result.fork();
                result.skip(4);
                copyNU(shr, shw, nu, in, result.os());

                savePoint.dout().writeInt(savePoint.remaining() - result.remaining() - 4);
            } else {
                result.dout().writeInt(len);
                nu.write(result.os());
                IOUtils.copy(in, result.os());
            }
        }

        return result.flip();
    }

    public static void copyNU(SliceHeaderReader shr, SliceHeaderWriter shw, NALUnit nu, InputStream is, OutputStream os)
            throws IOException {
        InBits reader = new BitstreamReader(is);
        OutBits writer = new BitstreamWriter(os);

        SliceHeader sh = shr.read(nu, reader);
        sh.pic_parameter_set_id = 1;

        nu.write(os);
        shw.write(sh, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, writer);

        copyCABAC(writer, reader);
    }

    private static AvcCBox doSampleEntry(DemuxerTrack videoTrack, CompressedTrack outTrack) throws IOException {
        SampleEntry se = videoTrack.getSampleEntries()[0];

        AvcCBox avcC = Box.findFirst(se, AvcCBox.class, AvcCBox.fourcc());
        AvcCBox old = avcC.copy();

        for (PictureParameterSet pps : avcC.getPpsList()) {
            Assert.assertTrue(pps.entropy_coding_mode_flag);
            pps.seq_parameter_set_id = 1;
            pps.pic_parameter_set_id = 1;
        }

        for (SeqParameterSet sps : avcC.getSpsList()) {
            sps.seq_parameter_set_id = 1;
        }

        return old;
    }

    private static void copyCABAC(OutBits w, InBits r) throws IOException {
        long bp = r.curBit();
        long rem = r.readNBit(8 - (int) bp);
        Assert.assertEquals((1 << (8 - bp)) - 1, rem);

        if (w.curBit() != 0)
            w.writeNBit(0xff, 8 - w.curBit());
        int b;
        while ((b = r.readNBit(8)) != -1)
            w.writeNBit(b, 8);
    }
}
