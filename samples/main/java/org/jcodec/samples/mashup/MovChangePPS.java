package org.jcodec.samples.mashup;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.containers.mp4.MP4TrackType.VIDEO;
import static org.jcodec.samples.mashup.MovStitch2.doFrame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
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

    public static void changePPS(File in, File out) throws IOException, FileNotFoundException {
        MP4Demuxer demuxer = new MP4Demuxer(readableChannel(in));
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(writableChannel(out));

        AbstractMP4DemuxerTrack videoTrack = demuxer.getVideoTrack();
        FramesMP4MuxerTrack outTrack = muxer.addTrack(VIDEO, (int) videoTrack.getTimescale());

        AvcCBox avcC = doSampleEntry(videoTrack, outTrack);

        SliceHeaderReader shr = new SliceHeaderReader();
        SeqParameterSet sps = SeqParameterSet.read(avcC.getSpsList().get(0));
        PictureParameterSet pps = PictureParameterSet.read(avcC.getPpsList().get(0));
        SliceHeaderWriter shw = new SliceHeaderWriter();

        for (int i = 0; i < videoTrack.getFrameCount(); i++) {
            MP4Packet packet = (MP4Packet)videoTrack.nextFrame();
            outTrack.addFrame(MP4Packet.createMP4PacketWithData(packet, doFrame(packet.getData(), shr, shw, sps, pps)));
        }

        muxer.writeHeader();
    }

    private static AvcCBox doSampleEntry(AbstractMP4DemuxerTrack videoTrack, FramesMP4MuxerTrack outTrack) throws IOException {
        SampleEntry se = videoTrack.getSampleEntries()[0];

        AvcCBox avcC = Box.findFirst(se, AvcCBox.class, AvcCBox.fourcc());
        AvcCBox old = (AvcCBox) MP4Util.cloneBox(avcC, 2048);

        for (int i = 0; i < avcC.getPpsList().size(); i++) {
            avcC.getPpsList().set(i, MovStitch2.updatePps(avcC.getPpsList().get(i)));
        }

        for (int i = 0; i < avcC.getSpsList().size(); i++) {
            avcC.getSpsList().set(i, MovStitch2.updateSps(avcC.getSpsList().get(i)));
        }

        outTrack.addSampleEntry(se);

        return old;
    }
}
