package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Track fragment header box
 * 
 * @author The JCodec project
 * 
 */
public class TrackFragmentHeaderBox extends FullBox {
    public TrackFragmentHeaderBox(Header atom) {
        super(atom);
    }

    //@formatter:off
    public static final int FLAG_BASE_DATA_OFFSET = 0x01;
    public static final int FLAG_SAMPLE_DESCRIPTION_INDEX = 0x02;
    public static final int FLAG_DEFAILT_SAMPLE_DURATION = 0x08;
    public static final int FLAG_DEFAULT_SAMPLE_SIZE = 0x10;
    public static final int FLAG_DEFAILT_SAMPLE_FLAGS = 0x20;
    //@formatter:on

    private int trackId;
    private long baseDataOffset;
    private int sampleDescriptionIndex;
    private int defaultSampleDuration;
    private int defaultSampleSize;
    private int defaultSampleFlags;

    public static String fourcc() {
        return "tfhd";
    }

    public static TrackFragmentHeaderBox createTrackFragmentHeaderBox(int trackId, long baseDataOffset,
            int sampleDescriptionIndex, int defaultSampleDuration, int defaultSampleSize, int defaultSampleFlags) {
        TrackFragmentHeaderBox box = new TrackFragmentHeaderBox(new Header(fourcc()));
        box.trackId = trackId;
        box.baseDataOffset = baseDataOffset;
        box.sampleDescriptionIndex = sampleDescriptionIndex;
        box.defaultSampleDuration = defaultSampleDuration;
        box.defaultSampleSize = defaultSampleSize;
        box.defaultSampleFlags = defaultSampleFlags;
        return box;
    }

    public static Factory create(int trackId) {
        return new Factory(TrackFragmentHeaderBox.createTrackFragmentHeaderBoxWithId(trackId));
    }

    public static Factory copy(TrackFragmentHeaderBox other) {
        TrackFragmentHeaderBox box = TrackFragmentHeaderBox
                .createTrackFragmentHeaderBox(other.trackId, other.baseDataOffset, other.sampleDescriptionIndex, other.defaultSampleDuration, other.defaultSampleSize, other.defaultSampleFlags);
        box.setFlags(other.getFlags());
        box.setVersion(other.getVersion());
        return new Factory(box);
    }

    public static TrackFragmentHeaderBox createTrackFragmentHeaderBoxWithId(int trackId) {
        TrackFragmentHeaderBox box = new TrackFragmentHeaderBox(new Header(fourcc()));
        box.trackId = trackId;
        return box;
    }

    public static class Factory {

        private TrackFragmentHeaderBox box;

        public Factory(TrackFragmentHeaderBox box) {
            this.box = box;
        }

        public Factory baseDataOffset(long baseDataOffset) {
            box.flags |= FLAG_BASE_DATA_OFFSET;
            box.baseDataOffset = (int) baseDataOffset;
            return this;
        }

        public Factory sampleDescriptionIndex(long sampleDescriptionIndex) {
            box.flags |= FLAG_SAMPLE_DESCRIPTION_INDEX;
            box.sampleDescriptionIndex = (int) sampleDescriptionIndex;
            return this;
        }

        public Factory defaultSampleDuration(long defaultSampleDuration) {
            box.flags |= FLAG_DEFAILT_SAMPLE_DURATION;
            box.defaultSampleDuration = (int) defaultSampleDuration;
            return this;
        }

        public Factory defaultSampleSize(long defaultSampleSize) {
            box.flags |= FLAG_DEFAULT_SAMPLE_SIZE;
            box.defaultSampleSize = (int) defaultSampleSize;
            return this;
        }

        public Factory defaultSampleFlags(long defaultSampleFlags) {
            box.flags |= FLAG_DEFAILT_SAMPLE_FLAGS;
            box.defaultSampleFlags = (int) defaultSampleFlags;
            return this;
        }

        public TrackFragmentHeaderBox create() {
            try {
                return box;
            } finally {
                box = null;
            }
        }
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        trackId = input.getInt();
        if (isBaseDataOffsetAvailable())
            baseDataOffset = input.getLong();
        if (isSampleDescriptionIndexAvailable())
            sampleDescriptionIndex = input.getInt();
        if (isDefaultSampleDurationAvailable())
            defaultSampleDuration = input.getInt();
        if (isDefaultSampleSizeAvailable())
            defaultSampleSize = input.getInt();
        if (isDefaultSampleFlagsAvailable())
            defaultSampleFlags = input.getInt();
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(trackId);
        if (isBaseDataOffsetAvailable())
            out.putLong(baseDataOffset);
        if (isSampleDescriptionIndexAvailable())
            out.putInt(sampleDescriptionIndex);
        if (isDefaultSampleDurationAvailable())
            out.putInt(defaultSampleDuration);
        if (isDefaultSampleSizeAvailable())
            out.putInt(defaultSampleSize);
        if (isDefaultSampleFlagsAvailable())
            out.putInt(defaultSampleFlags);
    }
    
    @Override
    public int estimateSize() {
        return 40;
    }

    public int getTrackId() {
        return trackId;
    }

    public long getBaseDataOffset() {
        return baseDataOffset;
    }

    public int getSampleDescriptionIndex() {
        return sampleDescriptionIndex;
    }

    public int getDefaultSampleDuration() {
        return defaultSampleDuration;
    }

    public int getDefaultSampleSize() {
        return defaultSampleSize;
    }

    public int getDefaultSampleFlags() {
        return defaultSampleFlags;
    }

    public boolean isBaseDataOffsetAvailable() {
        return (flags & FLAG_BASE_DATA_OFFSET) != 0;
    }

    public boolean isSampleDescriptionIndexAvailable() {
        return (flags & FLAG_SAMPLE_DESCRIPTION_INDEX) != 0;
    }

    public boolean isDefaultSampleDurationAvailable() {
        return (flags & FLAG_DEFAILT_SAMPLE_DURATION) != 0;
    }

    public boolean isDefaultSampleSizeAvailable() {
        return (flags & FLAG_DEFAULT_SAMPLE_SIZE) != 0;
    }

    public boolean isDefaultSampleFlagsAvailable() {
        return (flags & FLAG_DEFAILT_SAMPLE_FLAGS) != 0;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public void setDefaultSampleFlags(int defaultSampleFlags) {
        this.defaultSampleFlags = defaultSampleFlags;
    }

    public static TrackFragmentHeaderBox createTrackFragmentHeaderBox() {
        return new TrackFragmentHeaderBox(new Header(fourcc()));
    }
}