package org.jcodec.api.awt;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.api.UnsupportedFormatException;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.javase.scale.AWTUtil;

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
public class AWTFrameGrab extends FrameGrab {
    public static AWTFrameGrab createAWTFrameGrab(SeekableByteChannel _in) throws IOException, JCodecException {
        FrameGrab fg = FrameGrab.createFrameGrab(_in);
        return new AWTFrameGrab(fg.getVideoTrack(), fg.getDecoder());
    }
    
    public AWTFrameGrab(SeekableDemuxerTrack videoTrack, ContainerAdaptor decoder) {
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
            return ((AWTFrameGrab) createAWTFrameGrab(ch).seekToSecondPrecise(second)).getFrameWithOrientation();
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
        return ((AWTFrameGrab) createAWTFrameGrab(file).seekToSecondPrecise(second)).getFrame();
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @return
     * @throws IOException
     */
    public BufferedImage getFrame() throws IOException {
        Picture nativeFrame = getNativeFrame();
        return nativeFrame == null ? null : AWTUtil.toBufferedImage(nativeFrame);
    }

    public BufferedImage getFrameWithOrientation() throws IOException {
        PictureWithMetadata nativeFrame = getNativeFrameWithMetadata();
        return nativeFrame == null ? null : AWTUtil.toBufferedImage(nativeFrame.getPicture(), nativeFrame.getOrientation());
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
            return ((AWTFrameGrab) createAWTFrameGrab(ch).seekToFramePrecise(frameNumber)).getFrame();
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
        return ((AWTFrameGrab) createAWTFrameGrab(file).seekToFramePrecise(frameNumber)).getFrame();
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
        return ((AWTFrameGrab) new AWTFrameGrab(vt, decoder).seekToFramePrecise(frameNumber)).getFrame();
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
        return ((AWTFrameGrab) new AWTFrameGrab(vt, decoder).seekToSecondPrecise(second)).getFrame();
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
        return ((AWTFrameGrab) new AWTFrameGrab(vt, decoder).seekToFrameSloppy(frameNumber)).getFrame();
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
        return ((AWTFrameGrab) new AWTFrameGrab(vt, decoder).seekToSecondSloppy(second)).getFrame();
    }

}
