package org.jcodec.common;

public class IntIntHistogram extends IntIntMap {
    private int maxBin = -1;

    public int max() {
        return maxBin;
    }

    public void increment(int bin) {
        int count = get(bin);
        count = count == Integer.MIN_VALUE ? 1 : 1 + count;
        put(bin, count);
        
        if (maxBin == -1)
            maxBin = bin;
        int maxCount = get(maxBin);
        if (count > maxCount) {
            maxBin = bin;
            maxCount = count;
        }
    }
}
