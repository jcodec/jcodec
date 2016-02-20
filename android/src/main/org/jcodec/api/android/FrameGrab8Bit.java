package org.jcodec.api.android;

import java.io.File;
import java.io.IOException;

import org.jcodec.api.JCodecException;
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
public class FrameGrab8Bit extends org.jcodec.api.FrameGrab8Bit {

    public FrameGrab8Bit(SeekableByteChannel in) throws IOException, JCodecException {
        super(in);
    }

    public FrameGrab8Bit(SeekableDemuxerTrack videoTrack,
            ContainerAdaptor decoder) {
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
    public static Bitmap getFrame(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableFileChannel(file);
            return ((FrameGrab8Bit)new FrameGrab8Bit(ch).seekToSecondPrecise(second)).getFrame();
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
    public static Bitmap getFrame(SeekableByteChannel file, double second) throws JCodecException, IOException {
        return ((FrameGrab8Bit)new FrameGrab8Bit(file).seekToSecondPrecise(second)).getFrame();
    }
    
    /**
     * Get frame at current position in AWT image
     * 
     * @return
     * @throws IOException
     */
    public Bitmap getFrame() throws IOException {
        return AndroidUtil.toBitmap8Bit(getNativeFrame());
    }
    
    /**
     * Get frame at current position in AWT image
     * 
     * @return
     * @throws IOException
     */
    public void getFrame(Bitmap bmp) throws IOException {
    	Picture8Bit picture = getNativeFrame();
        AndroidUtil.toBitmap8Bit(picture, bmp);
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
            ch = NIOUtils.readableFileChannel(file);
            return ((FrameGrab8Bit)new FrameGrab8Bit(ch).seekToFramePrecise(frameNumber)).getFrame();
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
        return ((FrameGrab8Bit)new FrameGrab8Bit(file).seekToFramePrecise(frameNumber)).getFrame();
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
        return ((FrameGrab8Bit)new FrameGrab8Bit(vt, decoder).seekToFramePrecise(frameNumber)).getFrame();
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
        return ((FrameGrab8Bit)new FrameGrab8Bit(vt, decoder).seekToSecondPrecise(second)).getFrame();
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
        return ((FrameGrab8Bit)new FrameGrab8Bit(vt, decoder).seekToFrameSloppy(frameNumber)).getFrame();
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
        return ((FrameGrab8Bit)new FrameGrab8Bit(vt, decoder).seekToSecondSloppy(second)).getFrame();
    }
}