package org.jcodec.api.awt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceMuxer {
    private SeekableByteChannel ch;
    private FramesMP4MuxerTrack outTrack;
    private int frameNo;
    private MP4Muxer muxer;
    private Size size;

    public SequenceMuxer(File out) throws IOException {
        this.ch = NIOUtils.writableChannel(out);

        // Muxer that will store the encoded frames
        muxer = MP4Muxer.createMP4Muxer(ch, Brand.MP4);

        // Add video track to muxer
        outTrack = muxer.addTrack(MP4TrackType.VIDEO, 25);
    }

    public void encodeImage(File png) throws IOException {
        if (size == null) {
            BufferedImage read = ImageIO.read(png);
            size = new Size(read.getWidth(), read.getHeight());
        }
        // Add packet to video track
        outTrack.addFrame(MP4Packet.createMP4Packet(NIOUtils.fetchFromFile(png), frameNo, 25, 1, frameNo, true, null,
                frameNo, frameNo, 0));

        frameNo++;
    }

    public void finish() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        outTrack.addSampleEntry(MP4Muxer.videoSampleEntry("png ", size, "JCodec"));

        // Write MP4 header and finalize recording
        muxer.writeHeader();
        NIOUtils.closeQuietly(ch);
    }
}
