package org.jcodec.api.android;

import android.graphics.Bitmap;

import org.jcodec.api.SequenceEncoder8Bit;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
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
    
    public static AndroidSequenceEncoder8Bit createSequenceEncoder8Bit(File out) throws IOException {
        return new AndroidSequenceEncoder8Bit(NIOUtils.writableChannel(out));
    }

    public AndroidSequenceEncoder8Bit(SeekableByteChannel ch) throws IOException {
        super(ch);
    }

    public void encodeImage(Bitmap bi) throws IOException {
        encodeNativeFrame(BitmapUtil.fromBitmap8Bit(bi));
    }
}