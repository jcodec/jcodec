package org.jcodec.common;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class RunLength {
    protected IntArrayList counts = new IntArrayList();

    public int estimateSize() {
        int[] counts = getCounts();
        int recCount = 0;
        for (int i = 0; i < counts.length; i++, recCount++) {
            int count = counts[i];
            while (count >= 0x100) {
                ++recCount;
                count -= 0x100;
            }
        }
        return recCount * recSize() + 4;

    }

    protected abstract int recSize();

    protected abstract void finish();

    public int[] getCounts() {
        finish();
        return counts.toArray();
    }

    public static class Integer extends RunLength {
        private static final int MIN_VALUE = java.lang.Integer.MIN_VALUE;

        private int lastValue = Integer.MIN_VALUE;
        private int count = 0;

        private IntArrayList values = new IntArrayList();

        public void add(int value) {
            if (lastValue == Integer.MIN_VALUE || lastValue != value) {
                if (lastValue != Integer.MIN_VALUE) {
                    values.add(lastValue);
                    counts.add(count);
                    count = 0;
                }
                lastValue = value;
            }
            ++count;
        }

        public int[] getValues() {
            finish();
            return values.toArray();
        }

        protected void finish() {
            if (lastValue != Integer.MIN_VALUE) {
                values.add(lastValue);
                counts.add(count);
                lastValue = Integer.MIN_VALUE;
                count = 0;
            }
        }

        public void serialize(ByteBuffer bb) {
            ByteBuffer dup = bb.duplicate();
            int[] counts = getCounts();
            int[] values = getValues();
            NIOUtils.skip(bb, 4);
            int recCount = 0;
            for (int i = 0; i < counts.length; i++, recCount++) {
                int count = counts[i];
                while (count >= 0x100) {
                    bb.put((byte) 0xff);
                    bb.putInt(values[i]);
                    ++recCount;
                    count -= 0x100;
                }
                bb.put((byte) (count - 1));
                bb.putInt(values[i]);
            }
            dup.putInt(recCount);
        }

        public static RunLength.Integer parse(ByteBuffer bb) {
            RunLength.Integer rl = new RunLength.Integer();
            int recCount = bb.getInt();
            for (int i = 0; i < recCount; i++) {
                int count = (bb.get() & 0xff) + 1;
                int value = bb.getInt();
                rl.counts.add(count);
                rl.values.add(value);
            }
            return rl;
        }

        protected int recSize() {
            return 5;
        }

        public int[] flattern() {
            int[] counts = getCounts();
            int total = 0;
            for (int i = 0; i < counts.length; i++) {
                total += counts[i];
            }
            int[] values = getValues();
            int[] result = new int[total];
            for (int i = 0, ind = 0; i < counts.length; i++) {
                for (int j = 0; j < counts[i]; j++, ind++)
                    result[ind] = values[i];
            }
            return result;
        }
    }

    public static class Long extends RunLength {
        private static final long MIN_VALUE = java.lang.Long.MIN_VALUE;

        private long lastValue = Long.MIN_VALUE;
        private int count = 0;

        private LongArrayList values = new LongArrayList();

        public void add(long value) {
            if (lastValue == Long.MIN_VALUE || lastValue != value) {
                if (lastValue != Long.MIN_VALUE) {
                    values.add(lastValue);
                    counts.add(count);
                    count = 0;
                }
                lastValue = value;
            }
            ++count;
        }

        public int[] getCounts() {
            finish();
            return counts.toArray();
        }

        public long[] getValues() {
            finish();
            return values.toArray();
        }

        protected void finish() {
            if (lastValue != Long.MIN_VALUE) {
                values.add(lastValue);
                counts.add(count);
                lastValue = Long.MIN_VALUE;
                count = 0;
            }
        }

        public void serialize(ByteBuffer bb) {
            ByteBuffer dup = bb.duplicate();
            int[] counts = getCounts();
            long[] values = getValues();
            NIOUtils.skip(bb, 4);
            int recCount = 0;
            for (int i = 0; i < counts.length; i++, recCount++) {
                int count = counts[i];
                while (count >= 0x100) {
                    bb.put((byte) 0xff);
                    bb.putLong(values[i]);
                    ++recCount;
                    count -= 0x100;
                }
                bb.put((byte) (count - 1));
                bb.putLong(values[i]);
            }
            dup.putInt(recCount);
        }

        public static RunLength.Long parse(ByteBuffer bb) {
            RunLength.Long rl = new RunLength.Long();
            int recCount = bb.getInt();
            for (int i = 0; i < recCount; i++) {
                int count = (bb.get() & 0xff) + 1;
                long value = bb.getLong();
                rl.counts.add(count);
                rl.values.add(value);
            }
            return rl;
        }

        @Override
        protected int recSize() {
            return 9;
        }

        public long[] flattern() {
            int[] counts = getCounts();
            int total = 0;
            for (int i = 0; i < counts.length; i++) {
                total += counts[i];
            }
            long[] values = getValues();
            long[] result = new long[total];
            for (int i = 0, ind = 0; i < counts.length; i++) {
                for (int j = 0; j < counts[i]; j++, ind++)
                    result[ind] = values[i];
            }
            return result;
        }
    }
}