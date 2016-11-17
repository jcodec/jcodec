package org.jcodec.samples.h264embed;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * JCodec sample code
 * 
 * This sample embeds text onto H.264 picture without full re-transcode cycle
 * 
 * @author The JCodec project
 * 
 */
public class H264EmbedMain {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("H264 Text Embed");
            System.out.println("Syntax: <in> <out>");
            return;
        }

        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {
            source = readableChannel(new File(args[0]));
            sink = writableChannel(new File(args[1]));

            MP4Demuxer demux = new MP4Demuxer(source);
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);

            EmbedTranscoder transcoder = new EmbedTranscoder();

            AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];

            FramesMP4MuxerTrack outTrack = muxer.addTrack(MP4TrackType.VIDEO, (int) inTrack.getTimescale());
            outTrack.addSampleEntry(ine);

            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            for (int i = 0; (inFrame = inTrack.nextFrame()) != null; i++) {
                ByteBuffer data = inFrame.getData();
                _out.clear();
                ByteBuffer result = transcoder.transcode(H264Utils.splitFrame(data), _out);
                outTrack.addFrame(MP4Packet.createMP4PacketWithData((MP4Packet)inFrame, result));

                if (i % 100 == 0)
                    System.out.println((i * 100 / totalFrames) + "%");
            }
            muxer.writeHeader();
        } finally {
            if (sink != null)
                sink.close();
            if (source != null)
                source.close();
        }
    }
}
