package org.jcodec.api.android;

import android.graphics.Bitmap;

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
public class SequenceEncoder8Bit extends org.jcodec.api.SequenceEncoder8Bit {

    public SequenceEncoder8Bit(File out) throws IOException {
        super(out);
    }

    public void encodeImage(Bitmap bi) throws IOException {
        encodeNativeFrame(BitmapUtil.fromBitmap8Bit(bi));
    }
}