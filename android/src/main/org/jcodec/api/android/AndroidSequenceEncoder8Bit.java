package org.jcodec.api.android;

import android.graphics.Bitmap;

import org.jcodec.api.SequenceEncoder8Bit;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.BitmapUtil;

import java.io.File;
import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AndroidSequenceEncoder8Bit extends SequenceEncoder8Bit {

    public static AndroidSequenceEncoder8Bit createSequenceEncoder8Bit(File out, int fps) throws IOException {
        return new AndroidSequenceEncoder8Bit(NIOUtils.writableChannel(out), Rational.R(fps, 1));
    }

    public static AndroidSequenceEncoder8Bit create25Fps(File out) throws IOException {
        return new AndroidSequenceEncoder8Bit(NIOUtils.writableChannel(out), Rational.R(25, 1));
    }

    public static AndroidSequenceEncoder8Bit create30Fps(File out) throws IOException {
        return new AndroidSequenceEncoder8Bit(NIOUtils.writableChannel(out), Rational.R(30, 1));
    }

    public static AndroidSequenceEncoder8Bit create2997Fps(File out) throws IOException {
        return new AndroidSequenceEncoder8Bit(NIOUtils.writableChannel(out), Rational.R(30000, 1001));
    }

    public static AndroidSequenceEncoder8Bit create24Fps(File out) throws IOException {
        return new AndroidSequenceEncoder8Bit(NIOUtils.writableChannel(out), Rational.R(24, 1));
    }

    public AndroidSequenceEncoder8Bit(SeekableByteChannel ch, Rational fps) throws IOException {
        super(ch, fps);
    }

    public void encodeImage(Bitmap bi) throws IOException {
        encodeNativeFrame(BitmapUtil.fromBitmap8Bit(bi));
    }
}