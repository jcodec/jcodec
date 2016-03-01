package org.jcodec.api.android;

import java.io.File;
import java.io.IOException;

import org.jcodec.api.FrameGrab8Bit;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata8Bit;
import org.jcodec.api.UnsupportedFormatException;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.Picture8Bit;

import android.graphics.Bitmap;

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
 * NOTE: Android specific routines
 * 
 * @author The JCodec project
 * 
 */
public class AndroidFrameGrab8Bit extends FrameGrab8Bit {

    public static AndroidFrameGrab8Bit createAndroidFrameGrab8Bit(SeekableByteChannel in)
            throws IOException, JCodecException {
        FrameGrab8Bit frameGrab = createFrameGrab8Bit(in);
        return new AndroidFrameGrab8Bit(frameGrab.getVideoTrack(), frameGrab.getDecoder());
    }

    public AndroidFrameGrab8Bit(SeekableDemuxerTrack videoTrack, ContainerAdaptor decoder) {
        super(videoTrack, decoder);
    }

    /**
     * Get frame at a specified second as AWT image
     * 
     * @param file
     * @param second
     * @return A decoded frame from a given point in video.
     * @throws IOException
     * @throws JCodecException
     */
    public static Bitmap getFrame(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return ((AndroidFrameGrab8Bit) createAndroidFrameGrab8Bit(ch).seekToSecondPrecise(second)).getFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified second as AWT image
     * 
     * @param file
     * @param second
     * @return A decoded frame from a given point in video.
     * @throws UnsupportedFormatException
     * @throws IOException
     */
    public static Bitmap getFrame(SeekableByteChannel file, double second) throws JCodecException, IOException {
        return ((AndroidFrameGrab8Bit) createAndroidFrameGrab8Bit(file).seekToSecondPrecise(second)).getFrame();
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @return A decoded frame with metadata.
     * @throws IOException
     */
    public BitmapWithMetadata getFrameWithMetadata() throws IOException {
        PictureWithMetadata8Bit pictureWithMeta = getNativeFrameWithMetadata();
        if (pictureWithMeta == null)
            return null;
        Bitmap bitmap = AndroidUtil.toBitmap8Bit(pictureWithMeta.getPicture());
        return new BitmapWithMetadata(bitmap, pictureWithMeta.getTimestamp(), pictureWithMeta.getDuration());
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @return A decoded frame.
     * @throws IOException
     */
    public Bitmap getFrame() throws IOException {
        return AndroidUtil.toBitmap8Bit(getNativeFrame());
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @throws IOException
     */
    public void getFrame(Bitmap bmp) throws IOException {
        Picture8Bit picture = getNativeFrame();
        AndroidUtil.toBitmap8Bit(picture, bmp);
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @return A decoded picture with metadata. A bitmap provided is used.
     * 
     * @throws IOException
     */
    public BitmapWithMetadata getFrameWithMetadata(Bitmap bmp) throws IOException {
        PictureWithMetadata8Bit pictureWithMetadata = getNativeFrameWithMetadata();
        if (pictureWithMetadata == null)
            return null;
        AndroidUtil.toBitmap8Bit(pictureWithMetadata.getPicture(), bmp);
        return new BitmapWithMetadata(bmp, pictureWithMetadata.getTimestamp(), pictureWithMetadata.getDuration());
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
    public static Bitmap getFrame(File file, int frameNumber) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return ((AndroidFrameGrab8Bit) createAndroidFrameGrab8Bit(ch).seekToFramePrecise(frameNumber)).getFrame();
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
    public static Bitmap getFrame(SeekableByteChannel file, int frameNumber) throws JCodecException, IOException {
        return ((AndroidFrameGrab8Bit) createAndroidFrameGrab8Bit(file).seekToFramePrecise(frameNumber)).getFrame();
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
    public static Bitmap getFrame(SeekableDemuxerTrack vt, ContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return ((AndroidFrameGrab8Bit) new AndroidFrameGrab8Bit(vt, decoder).seekToFramePrecise(frameNumber)).getFrame();
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
    public static Bitmap getFrame(SeekableDemuxerTrack vt, ContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return ((AndroidFrameGrab8Bit) new AndroidFrameGrab8Bit(vt, decoder).seekToSecondPrecise(second)).getFrame();
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
    public static Bitmap getFrameSloppy(SeekableDemuxerTrack vt, ContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return ((AndroidFrameGrab8Bit) new AndroidFrameGrab8Bit(vt, decoder).seekToFrameSloppy(frameNumber)).getFrame();
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
    public static Bitmap getFrameSloppy(SeekableDemuxerTrack vt, ContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return ((AndroidFrameGrab8Bit) new AndroidFrameGrab8Bit(vt, decoder).seekToSecondSloppy(second)).getFrame();
    }
}