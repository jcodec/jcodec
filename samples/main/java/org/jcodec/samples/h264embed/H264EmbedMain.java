package org.jcodec.samples.h264embed;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.MP4DemuxerTrack;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * JCodec sample code
 * 
 * This sample embeds text onto H.264 picture without full re-transcode cycle
 * 
 * @author Jay Codec
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
            source = new FileChannelWrapper(new File(args[0]));
            sink = new FileChannelWrapper(new File(args[1]));

            MP4Demuxer demux = new MP4Demuxer(source);
            MP4Muxer muxer = new MP4Muxer(sink, Brand.MOV);

            EmbedTranscoder transcoder = new EmbedTranscoder();

            MP4DemuxerTrack inTrack = demux.getVideoTrack();
            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];

            CompressedTrack outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, (int) inTrack.getTimescale());
            outTrack.addSampleEntry(ine);

            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            for (int i = 0; (inFrame = inTrack.getFrames(1)) != null; i++) {
                ByteBuffer data = inFrame.getData();
                H264Utils.decodeMOVPacket(data);
                _out.clear();
                ByteBuffer result = transcoder.transcode(data, _out);
                outTrack.addFrame(new MP4Packet((MP4Packet)inFrame, result));

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
