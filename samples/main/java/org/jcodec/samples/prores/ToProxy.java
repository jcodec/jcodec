package org.jcodec.samples.prores;

import static org.jcodec.common.NIOUtils.readableFileChannel;
import static org.jcodec.common.NIOUtils.writableFileChannel;

import java.io.File;
import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresToProxy;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Turns a ProRes frame into ProRes proxy
 * 
 * @author Jay Codec
 * 
 */
public class ToProxy {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: <in.mov> <out.mov>");
            return;
        }

        SeekableByteChannel input = readableFileChannel(new File(args[0]));
        MP4Demuxer demuxer = new MP4Demuxer(input);
        SeekableByteChannel output = writableFileChannel(new File(args[1]));
        MP4Muxer muxer = new MP4Muxer(output, Brand.MOV);

        AbstractMP4DemuxerTrack inVideo = demuxer.getVideoTrack();
        VideoSampleEntry entry = (VideoSampleEntry) inVideo.getSampleEntries()[0];
        int width = (int) entry.getWidth();
        int height = (int) entry.getHeight();
        ProresToProxy toProxy = new ProresToProxy(width, height, 65536);
        FramesMP4MuxerTrack outVideo = muxer.addTrack(TrackType.VIDEO, (int) inVideo.getTimescale());

        TrackHeaderBox th = inVideo.getBox().getTrackHeader();
        System.out.println(toProxy.getFrameSize());
        int frame = 0;
        long from = System.currentTimeMillis();
        long last = from;
        MP4Packet pkt = null;
        while ((pkt = (MP4Packet)inVideo.nextFrame()) != null) {
            ByteBuffer out = ByteBuffer.allocate(pkt.getData().remaining());
            toProxy.transcode(pkt.getData(), out);
            out.flip();
            outVideo.addFrame(new MP4Packet(pkt, out));
            frame++;
            long cur = System.currentTimeMillis();
            if (cur - last > 5000) {
                System.out.println(((1000 * frame) / (cur - from)) + " fps");
                last = cur;
            }
        }
        entry.setMediaType("apco");
        outVideo.addSampleEntry(entry);

        muxer.writeHeader();
        output.close();
        input.close();
    }
}