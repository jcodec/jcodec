package org.jcodec.api;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
@Deprecated
public class SequenceEncoder {
    private SeekableByteChannel ch;
    private Picture toEncode;
    private Transform transform;
    private H264Encoder encoder;
    private MuxerTrack outTrack;
    private ByteBuffer _out;
    private int frameNo;
    private MP4Muxer muxer;
    private ByteBuffer sps;
    private ByteBuffer pps;

    public static SequenceEncoder createSequenceEncoder(File out) throws IOException {
        return new SequenceEncoder(NIOUtils.writableChannel(out));
    }

    public SequenceEncoder(SeekableByteChannel ch) throws IOException {
        this.ch = ch;

        // Muxer that will store the encoded frames
        muxer = MP4Muxer.createMP4Muxer(ch, Brand.MP4);

        // Allocate a buffer big enough to hold output frames
        _out = ByteBuffer.allocate(1920 * 1080 * 6);

        // Create an instance of encoder
        encoder = H264Encoder.createH264Encoder();

        // Transform to convert between RGB and YUV
        transform = ColorUtil.getTransform(ColorSpace.RGB, encoder.getSupportedColorSpaces()[0]);
    }

    public void encodeNativeFrame(Picture pic) throws IOException {
        if (toEncode == null) {
            toEncode = Picture.create(pic.getWidth(), pic.getHeight(), encoder.getSupportedColorSpaces()[0]);
        }
        if (outTrack == null) {
            // Add video track to muxer
            outTrack = muxer.addVideoTrack(Codec.H264, new VideoCodecMeta(new Size(pic.getWidth(), pic.getHeight()), pic.getColor()));
        }

        // Perform conversion
        transform.transform(pic, toEncode);

        // Encode image into H.264 frame, the result is stored in '_out' buffer
        _out.clear();
        EncodedFrame ef = encoder.encodeFrame(toEncode, _out);
        ByteBuffer result = ef.getData();

        // Add packet to video track
        outTrack.addFrame(Packet.createPacket(result, frameNo, 25, 1, frameNo,
                ef.isKeyFrame() ? FrameType.KEY : FrameType.INTER, null));

        frameNo++;
    }

    public H264Encoder getEncoder() {
        return encoder;
    }

    public void finish() throws IOException {
        // Write MP4 header and finalize recording
        muxer.finish();
        NIOUtils.closeQuietly(ch);
    }
}
