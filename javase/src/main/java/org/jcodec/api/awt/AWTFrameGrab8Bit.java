package org.jcodec.api.awt;

import org.jcodec.api.FrameGrab8Bit;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata8Bit;
import org.jcodec.api.UnsupportedFormatException;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts frames from a movie into uncompressed images suitable for
 * processing.
 * 
 * Supports going to random points inside of a movie ( seeking ) by frame number
 * of by second.
 * 
 * NOTE: Supports only AVC ( H.264 ) in MP4 ( ISO BMF, QuickTime ) at this
 * point.
 * 
 * NOTE: AWT specific routines
 * 
 * @author The JCodec project
 * 
 */
public class AWTFrameGrab8Bit extends FrameGrab8Bit {
    public static AWTFrameGrab8Bit createAWTFrameGrab8Bit(SeekableByteChannel _in) throws IOException, JCodecException {
        FrameGrab8Bit fg = FrameGrab8Bit.createFrameGrab8Bit(_in);
        return new AWTFrameGrab8Bit(fg.getVideoTrack(), fg.getDecoder());
    }
    
    public AWTFrameGrab8Bit(SeekableDemuxerTrack videoTrack, ContainerAdaptor decoder) {
        super(videoTrack, decoder);
    }

    /**
     * Get frame at a specified second as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return ((AWTFrameGrab8Bit) createAWTFrameGrab8Bit(ch).seekToSecondPrecise(second)).getFrameWithOrientation();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified second as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws UnsupportedFormatException
     * @throws IOException
     */
    public static BufferedImage getFrame(SeekableByteChannel file, double second) throws JCodecException, IOException {
        return ((AWTFrameGrab8Bit) createAWTFrameGrab8Bit(file).seekToSecondPrecise(second)).getFrame();
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @return
     * @throws IOException
     */
    public BufferedImage getFrame() throws IOException {
        Picture8Bit nativeFrame = getNativeFrame();
        return nativeFrame == null ? null : AWTUtil.toBufferedImage8Bit(nativeFrame);
    }

    public BufferedImage getFrameWithOrientation() throws IOException {
        PictureWithMetadata8Bit nativeFrame = getNativeFrameWithMetadata();
        return nativeFrame == null ? null : AWTUtil.toBufferedImage8Bit(nativeFrame.getPicture(), nativeFrame.getOrientation());
    }

    /**
     * Get frame at a specified frame number as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(File file, int frameNumber) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return ((AWTFrameGrab8Bit) createAWTFrameGrab8Bit(ch).seekToFramePrecise(frameNumber)).getFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified frame number as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(SeekableByteChannel file, int frameNumber) throws JCodecException, IOException {
        return ((AWTFrameGrab8Bit) createAWTFrameGrab8Bit(file).seekToFramePrecise(frameNumber)).getFrame();
    }

    /**
     * Get a specified frame by number from an already open demuxer track
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(SeekableDemuxerTrack vt, ContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return ((AWTFrameGrab8Bit) new AWTFrameGrab8Bit(vt, decoder).seekToFramePrecise(frameNumber)).getFrame();
    }

    /**
     * Get a specified frame by second from an already open demuxer track
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(SeekableDemuxerTrack vt, ContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return ((AWTFrameGrab8Bit) new AWTFrameGrab8Bit(vt, decoder).seekToSecondPrecise(second)).getFrame();
    }

    /**
     * Get a specified frame by number from an already open demuxer track (
     * sloppy mode, i.e. nearest keyframe )
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrameSloppy(SeekableDemuxerTrack vt, ContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return ((AWTFrameGrab8Bit) new AWTFrameGrab8Bit(vt, decoder).seekToFrameSloppy(frameNumber)).getFrame();
    }

    /**
     * Get a specified frame by second from an already open demuxer track (
     * sloppy mode, i.e. nearest keyframe )
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrameSloppy(SeekableDemuxerTrack vt, ContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return ((AWTFrameGrab8Bit) new AWTFrameGrab8Bit(vt, decoder).seekToSecondSloppy(second)).getFrame();
    }

}
