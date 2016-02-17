package org.jcodec.api.android;

import android.graphics.Bitmap;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BitmapWithMetadata {
    private Bitmap bitmap;
    private double timestamp;
    private double duration;

    public BitmapWithMetadata(Bitmap bitmap, double pts, double duration) {
        this.bitmap = bitmap;
        this.timestamp = pts;
        this.duration = duration;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public double getTimestamp() {
        return timestamp;
    }
    
    public double getDuration() {
        return duration;
    }
}
