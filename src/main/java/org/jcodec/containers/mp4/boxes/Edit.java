package org.jcodec.containers.mp4.boxes;

import org.jcodec.containers.mp4.boxes.Box.AtomField;

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

    @AtomField(idx=0)
    public long getDuration() {
        return duration;
    }

    @AtomField(idx=1)
    public long getMediaTime() {
        return mediaTime;
    }

    @AtomField(idx=2)
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
