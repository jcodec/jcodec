package org.jcodec.samples.prores;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresToProxy;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Turns a ProRes frame into ProRes proxy
 * 
 * @author The JCodec project
 * 
 */
public class ToProxy {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: <in.mov> <out.mov>");
            return;
        }

        SeekableByteChannel input = readableChannel(new File(args[0]));
        MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(input);
        SeekableByteChannel output = writableChannel(new File(args[1]));
        MP4Muxer muxer = MP4Muxer.createMP4Muxer(output, Brand.MOV);

        DemuxerTrack inVideo = demuxer.getVideoTrack();
        DemuxerTrackMeta meta = inVideo.getMeta();
        Size size = meta.getCodecMeta().video().getSize();
        ProresToProxy toProxy = new ProresToProxy(size.getWidth(), size.getHeight(), 65536);
        MuxerTrack outVideo = muxer.addVideoTrack(meta.getCodecMeta().video());

        System.out.println(toProxy.getFrameSize());
        int frame = 0;
        long from = System.currentTimeMillis();
        long last = from;
        MP4Packet pkt = null;
        while ((pkt = (MP4Packet)inVideo.nextFrame()) != null) {
            ByteBuffer out = ByteBuffer.allocate(pkt.getData().remaining());
            toProxy.transcode(pkt.getData(), out);
            out.flip();
            outVideo.addFrame(MP4Packet.createMP4PacketWithData(pkt, out));
            frame++;
            long cur = System.currentTimeMillis();
            if (cur - last > 5000) {
                System.out.println(((1000 * frame) / (cur - from)) + " fps");
                last = cur;
            }
        }

        muxer.finish();
        output.close();
        input.close();
    }
}