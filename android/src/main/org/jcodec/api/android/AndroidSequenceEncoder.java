package org.jcodec.api.android;

import android.graphics.Bitmap;

import org.jcodec.api.SequenceEncoder;
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
 * @deprecated use {@link org.jcodec.api.android.AndroidSequenceEncoder8Bit} instead.
 */
@Deprecated
public class AndroidSequenceEncoder extends SequenceEncoder {

    public static AndroidSequenceEncoder createSequenceEncoder(File out) throws IOException {
        return new AndroidSequenceEncoder(NIOUtils.writableChannel(out));
    }

    public AndroidSequenceEncoder(SeekableByteChannel out) throws IOException {
        super(out);
    }

    public void encodeImage(Bitmap bi) throws IOException {
        encodeNativeFrame(BitmapUtil.fromBitmap(bi));
    }
}