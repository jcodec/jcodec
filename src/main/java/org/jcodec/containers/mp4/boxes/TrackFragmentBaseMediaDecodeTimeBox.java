package org.jcodec.containers.mp4.boxes;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The Track Fragment Base Media Decode Time Box provides the absolute decode
 * time, measured on the media timeline, of the first sample in decode order in
 * the track fragment. This can be useful, for example, when performing random
 * access in a file; it is not necessary to sum the sample durations of all
 * preceding samples in previous fragments to find this value (where the sample
 * durations are the deltas in the Decoding Time to Sample Box and the
 * sample_durations in the preceding track runs). The Track Fragment Base Media
 * Decode Time Box, if present, shall be positioned after the Track Fragment
 * Header Box and before the first Track Fragment Run box.
 * 
 * @author The JCodec project
 * 
 */
public class TrackFragmentBaseMediaDecodeTimeBox extends FullBox {

    public TrackFragmentBaseMediaDecodeTimeBox(Header atom) {
        super(atom);
    }

    private long baseMediaDecodeTime;

    public static TrackFragmentBaseMediaDecodeTimeBox createTrackFragmentBaseMediaDecodeTimeBox(
            long baseMediaDecodeTime) {
        TrackFragmentBaseMediaDecodeTimeBox box = new TrackFragmentBaseMediaDecodeTimeBox(new Header(fourcc()));
        box.baseMediaDecodeTime = baseMediaDecodeTime;
        if (box.baseMediaDecodeTime > Integer.MAX_VALUE) {
            box.version = 1;
        }
        return box;
    }

    public static String fourcc() {
        return "tfdt";
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        if (version == 0) {
            baseMediaDecodeTime = input.getInt();
        } else if (version == 1) {
            baseMediaDecodeTime = input.getLong();
        } else
            throw new RuntimeException("Unsupported tfdt version");
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        if (version == 0) {
            out.putInt((int) baseMediaDecodeTime);
        } else if (version == 1) {
            out.putLong(baseMediaDecodeTime);
        } else
            throw new RuntimeException("Unsupported tfdt version");
    }

    public long getBaseMediaDecodeTime() {
        return baseMediaDecodeTime;
    }

    public void setBaseMediaDecodeTime(long baseMediaDecodeTime) {
        this.baseMediaDecodeTime = baseMediaDecodeTime;
    }

    public static Factory copy(TrackFragmentBaseMediaDecodeTimeBox other) {
        return new Factory(other);
    }

    public static class Factory {
        private TrackFragmentBaseMediaDecodeTimeBox box;

        protected Factory(TrackFragmentBaseMediaDecodeTimeBox other) {
            box = TrackFragmentBaseMediaDecodeTimeBox
                    .createTrackFragmentBaseMediaDecodeTimeBox(other.baseMediaDecodeTime);
            box.version = other.version;
            box.flags = other.flags;
        }

        public Factory baseMediaDecodeTime(long val) {
            box.baseMediaDecodeTime = val;
            return this;
        }

        public TrackFragmentBaseMediaDecodeTimeBox create() {
            try {
                return box;
            } finally {
                box = null;
            }
        }
    }
}