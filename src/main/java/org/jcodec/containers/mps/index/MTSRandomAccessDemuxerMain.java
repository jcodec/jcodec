package org.jcodec.containers.mps.index;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.index.MPSRandomAccessDemuxer.Stream;

public class MTSRandomAccessDemuxerMain {

    public static void main(String[] args) throws IOException {
        MTSIndexer indexer = new MTSIndexer();
        File source = new File(args[0]);

        MTSIndex index;
        File indexFile = new File(source.getParentFile(), source.getName() + ".idx");
        if (!indexFile.exists()) {

            indexer.index(source, null);
            index = indexer.serialize();
            NIOUtils.writeTo(index.serialize(), indexFile);
        } else {
            System.out.println("Reading index from: " + indexFile.getName());
            index = MTSIndex.parse(NIOUtils.fetchFromFile(indexFile));
        }

        MTSRandomAccessDemuxer demuxer = new MTSRandomAccessDemuxer(NIOUtils.readableChannel(source), index);
        int[] guids = demuxer.getGuids();

        Stream video = getVideoStream(demuxer.getProgramDemuxer(guids[0]));

        FileChannelWrapper ch = NIOUtils.writableChannel(new File(args[1]));
        MP4Muxer mp4Muxer = new MP4Muxer(ch, Brand.MOV);
        FramesMP4MuxerTrack videoTrack = mp4Muxer.addVideoTrack("m2v1", new Size(1920, 1080), "jcod", 90000);

        video.gotoSyncFrame(175);
        Packet pkt = video.nextFrame();
        long firstPts = pkt.getPts();
        for (int i = 0; pkt != null && i < 150; i++) {
            videoTrack.addFrame(new MP4Packet(pkt.getData(), pkt.getPts() - firstPts, pkt.getTimescale(), pkt
                    .getDuration(), pkt.getFrameNo(), pkt.isKeyFrame(), pkt.getTapeTimecode(), pkt.getPts() - firstPts,
                    0));
            pkt = video.nextFrame();
        }
        mp4Muxer.writeHeader();
        NIOUtils.closeQuietly(ch);
    }

    private static Stream getVideoStream(MPSRandomAccessDemuxer demuxer) {
        Stream[] streams = demuxer.getStreams();
        for (Stream stream : streams) {
            if (stream.getStreamId() >= 0xe0 && stream.getStreamId() <= 0xef)
                return stream;
        }
        return null;
    }
}
