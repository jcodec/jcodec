package org.jcodec.samples.mashup;

import static org.jcodec.common.JCodecUtil.bufin;
import static org.jcodec.containers.mp4.TrackType.VIDEO;
import static org.jcodec.samples.mashup.MovStitch2.doFrame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Assert;

import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.read.SliceHeaderReader;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.FileRAOutputStream;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.DemuxerTrack;
import org.jcodec.containers.mp4.MP4DemuxerException;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.SampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class MovChangePPS {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: <in.mov> <out.mov>");
            return;
        }

        File in = new File(args[0]);
        File out = new File(args[1]);

        changePPS(in, out);
    }

    public static void changePPS(File in, File out) throws IOException, MP4DemuxerException, FileNotFoundException {
        MP4Demuxer demuxer = new MP4Demuxer(bufin(in));
        MP4Muxer muxer = new MP4Muxer(new FileRAOutputStream(out));

        DemuxerTrack videoTrack = demuxer.getVideoTrack();
        CompressedTrack outTrack = muxer.addTrackForCompressed(VIDEO, (int) videoTrack.getTimescale());

        AvcCBox avcC = doSampleEntry(videoTrack, outTrack);

        SliceHeaderReader shr = new SliceHeaderReader(avcC);
        SliceHeaderWriter shw = new SliceHeaderWriter(avcC.getSPS(0), avcC.getPPS(0));

        for (int i = 0; i < videoTrack.getFrameCount(); i++) {
            MP4Packet packet = videoTrack.getFrames(1);
            outTrack.addFrame(new MP4Packet(packet, doFrame(packet.getData(), shr, shw)));
        }

        muxer.writeHeader();
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

        outTrack.addSampleEntry(se);

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
