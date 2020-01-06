package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class Edit {
    public static Edit createEdit(Edit edit) {
        return new Edit(edit.duration, edit.mediaTime, edit.rate);
    }

    private long duration;
    private long mediaTime;
    private float rate;
    
    public Edit(long duration, long mediaTime, float rate) {
        this.duration = duration;
        this.mediaTime = mediaTime;
        this.rate = rate;
    }

    public long getDuration() {
        return duration;
    }

    public long getMediaTime() {
        return mediaTime;
    }

    public float getRate() {
        return rate;
    }

    public void shift(long shift) {
        mediaTime += shift;
    }

    public void setMediaTime(long l) {
        mediaTime = l;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

	public void stretch(long l) {
		this.duration += l;
	}
}
