package org.jcodec.common;

public class Ints {
    /**
     * Returns the {@code int} value that is equal to {@code value}, if possible.
     *
     * @param value any value in the range of the {@code int} type
     * @return the {@code int} value that equals {@code value}
     * @throws IllegalArgumentException if {@code value} is greater than {@link
     *     Integer#MAX_VALUE} or less than {@link Integer#MIN_VALUE}
     */
    public static int checkedCast(long value) {
      int result = (int) value;
      if (result != value) {
        // don't use checkArgument here, to avoid boxing
        throw new IllegalArgumentException("Out of range: " + value);
      }
      return result;
    }
}
