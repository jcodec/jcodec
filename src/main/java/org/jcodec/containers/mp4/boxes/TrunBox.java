package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.List;

//@formatter:off
/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Track fragment run
 * 
 * To crate new box:
 * 
 * <pre>
 * 
 * Box box = TrunBox
 *         .create(2)
 *         .dataOffset(20)
 *         .sampleCompositionOffset(new int[] { 11, 12 })
 *         .sampleDuration(new int[] { 15, 16 })
 *         .sampleFlags(new int[] { 100, 200 })
 *         .sampleSize(new int[] { 30, 40 })
 *         .create();
 * 
 * </pre>
 * 
 * @author The JCodec project
 * 
 */
//@formatter:on
public class TrunBox extends FullBox {
    // @formatter:off
    private static final int DATA_OFFSET_AVAILABLE = 0x000001;
    private static final int FIRST_SAMPLE_FLAGS_AVAILABLE = 0x000004;
    private static final int SAMPLE_DURATION_AVAILABLE = 0x000100;
    private static final int SAMPLE_SIZE_AVAILABLE = 0x000200;
    private static final int SAMPLE_FLAGS_AVAILABLE = 0x000400;
    private static final int SAMPLE_COMPOSITION_OFFSET_AVAILABLE = 0x000800;
    // @formatter:on

    private int sampleCount;
    private int dataOffset;
    private int firstSampleFlags;
    private int[] sampleDuration;
    private int[] sampleSize;
    private int[] sampleFlags;
    private int[] sampleCompositionOffset;

    public static String fourcc() {
        return "trun";
    }

    public void setDataOffset(int dataOffset) {
        this.dataOffset = dataOffset;
    }

    public static Factory create(int sampleCount) {
        return new Factory(TrunBox.createTrunBox1(sampleCount));
    }

    public static Factory copy(TrunBox other) {
        TrunBox box = TrunBox
                .createTrunBox2(other.sampleCount, other.dataOffset, other.firstSampleFlags, other.sampleDuration, other.sampleSize, other.sampleFlags, other.sampleCompositionOffset);
        box.setFlags(other.getFlags());
        box.setVersion(other.getVersion());
        return new Factory(box);
    }

    public TrunBox(Header header) {
        super(header);
    }

    public static TrunBox createTrunBox1(int sampleCount) {
        TrunBox trun = new TrunBox(new Header(fourcc()));
        trun.sampleCount = sampleCount;
        return trun;
    }

    public static TrunBox createTrunBox2(int sampleCount, int dataOffset, int firstSampleFlags, int[] sampleDuration,
            int[] sampleSize, int[] sampleFlags, int[] sampleCompositionOffset) {
        TrunBox trun = new TrunBox(new Header(fourcc()));
        trun.sampleCount = sampleCount;
        trun.dataOffset = dataOffset;
        trun.firstSampleFlags = firstSampleFlags;
        trun.sampleDuration = sampleDuration;
        trun.sampleSize = sampleSize;
        trun.sampleFlags = sampleFlags;
        trun.sampleCompositionOffset = sampleCompositionOffset;
        return trun;
    }

    public static class Factory {

        private TrunBox box;

        protected Factory(TrunBox box) {
            this.box = box;
        }

        public Factory dataOffset(long dataOffset) {
            box.flags |= DATA_OFFSET_AVAILABLE;
            box.dataOffset = (int) dataOffset;
            return this;
        }

        public Factory firstSampleFlags(int firstSampleFlags) {
            if (box.isSampleFlagsAvailable())
                throw new IllegalStateException("Sample flags already set on this object");
            box.flags |= FIRST_SAMPLE_FLAGS_AVAILABLE;
            box.firstSampleFlags = firstSampleFlags;
            return this;
        }

        public Factory sampleDuration(int[] sampleDuration) {
            if (sampleDuration.length != box.sampleCount)
                throw new IllegalArgumentException("Argument array length not equal to sampleCount");
            box.flags |= SAMPLE_DURATION_AVAILABLE;
            box.sampleDuration = sampleDuration;
            return this;
        }

        public Factory sampleSize(int[] sampleSize) {
            if (sampleSize.length != box.sampleCount)
                throw new IllegalArgumentException("Argument array length not equal to sampleCount");
            box.flags |= SAMPLE_SIZE_AVAILABLE;
            box.sampleSize = sampleSize;
            return this;
        }

        public Factory sampleFlags(int[] sampleFlags) {
            if (sampleFlags.length != box.sampleCount)
                throw new IllegalArgumentException("Argument array length not equal to sampleCount");
            if (box.isFirstSampleFlagsAvailable())
                throw new IllegalStateException("First sample flags already set on this object");
            box.flags |= SAMPLE_FLAGS_AVAILABLE;
            box.sampleFlags = sampleFlags;
            return this;
        }

        public Factory sampleCompositionOffset(int[] sampleCompositionOffset) {
            if (sampleCompositionOffset.length != box.sampleCount)
                throw new IllegalArgumentException("Argument array length not equal to sampleCount");
            box.flags |= SAMPLE_COMPOSITION_OFFSET_AVAILABLE;
            box.sampleCompositionOffset = sampleCompositionOffset;
            return this;
        }

        public TrunBox create() {
            try {
                return box;
            } finally {
                box = null;
            }
        }
    }

    public long getSampleCount() {
        return sampleCount & 0xffffffffL;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getFirstSampleFlags() {
        return firstSampleFlags;
    }

    public int[] getSampleDurations() {
        return sampleDuration;
    }

    public int[] getSampleSizes() {
        return sampleSize;
    }

    public int[] getSamplesFlags() {
        return sampleFlags;
    }

    public int[] getSampleCompositionOffsets() {
        return sampleCompositionOffset;
    }

    public long getSampleDuration(int i) {
        return sampleDuration[i] & 0xffffffffL;
    }

    public long getSampleSize(int i) {
        return sampleSize[i] & 0xffffffffL;
    }

    public int getSampleFlags(int i) {
        return sampleFlags[i];
    }

    public long getSampleCompositionOffset(int i) {
        return sampleCompositionOffset[i] & 0xffffffffL;
    }

    public boolean isDataOffsetAvailable() {
        return (flags & DATA_OFFSET_AVAILABLE) != 0;
    }

    public boolean isSampleCompositionOffsetAvailable() {
        return (flags & SAMPLE_COMPOSITION_OFFSET_AVAILABLE) != 0;
    }

    public boolean isSampleFlagsAvailable() {
        return (flags & SAMPLE_FLAGS_AVAILABLE) != 0;
    }

    public boolean isSampleSizeAvailable() {
        return (flags & SAMPLE_SIZE_AVAILABLE) != 0;
    }

    public boolean isSampleDurationAvailable() {
        return (flags & SAMPLE_DURATION_AVAILABLE) != 0;
    }

    public boolean isFirstSampleFlagsAvailable() {
        return (flags & FIRST_SAMPLE_FLAGS_AVAILABLE) != 0;
    }

    public static int flagsGetSampleDependsOn(int flags) {
        return (flags >> 6) & 0x3;
    }

    public static int flagsGetSampleIsDependedOn(int flags) {
        return (flags >> 8) & 0x3;
    }

    public static int flagsGetSampleHasRedundancy(int flags) {
        return (flags >> 10) & 0x3;
    }

    public static int flagsGetSamplePaddingValue(int flags) {
        return (flags >> 12) & 0x7;
    }

    public static int flagsGetSampleIsDifferentSample(int flags) {
        return (flags >> 15) & 0x1;
    }

    public static int flagsGetSampleDegradationPriority(int flags) {
        return (flags >> 16) & 0xffff;
    }

    public static TrunBox createTrunBox() {
        return new TrunBox(new Header(fourcc()));
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);

        if (isSampleFlagsAvailable() && isFirstSampleFlagsAvailable())
            throw new RuntimeException("Broken stream");

        sampleCount = input.getInt();
        if (isDataOffsetAvailable())
            dataOffset = input.getInt();
        if (isFirstSampleFlagsAvailable())
            firstSampleFlags = input.getInt();
        if (isSampleDurationAvailable())
            sampleDuration = new int[sampleCount];
        if (isSampleSizeAvailable())
            sampleSize = new int[sampleCount];
        if (isSampleFlagsAvailable())
            sampleFlags = new int[sampleCount];
        if (isSampleCompositionOffsetAvailable())
            sampleCompositionOffset = new int[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            if (isSampleDurationAvailable())
                sampleDuration[i] = input.getInt();
            if (isSampleSizeAvailable())
                sampleSize[i] = input.getInt();
            if (isSampleFlagsAvailable())
                sampleFlags[i] = input.getInt();
            if (isSampleCompositionOffsetAvailable())
                sampleCompositionOffset[i] = input.getInt();
        }
    }

    @Override
    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(sampleCount);
        if (isDataOffsetAvailable())
            out.putInt(dataOffset);
        if (isFirstSampleFlagsAvailable())
            out.putInt(firstSampleFlags);

        for (int i = 0; i < sampleCount; i++) {
            if (isSampleDurationAvailable())
                out.putInt(sampleDuration[i]);
            if (isSampleSizeAvailable())
                out.putInt(sampleSize[i]);
            if (isSampleFlagsAvailable())
                out.putInt(sampleFlags[i]);
            if (isSampleCompositionOffsetAvailable())
                out.putInt(sampleCompositionOffset[i]);
        }
    }

    protected void getModelFields(List<String> model) {
        model.add("sampleCount");
        if (isDataOffsetAvailable())
            model.add("dataOffset");

        if (isFirstSampleFlagsAvailable())
            model.add("firstSampleFlags");

        if (isSampleDurationAvailable())
            model.add("sampleDuration");

        if (isSampleSizeAvailable())
            model.add("sampleSize");

        if (isSampleFlagsAvailable())
            model.add("sampleFlags");

        if (isSampleCompositionOffsetAvailable())
            model.add("sampleCompositionOffset");
    }
}