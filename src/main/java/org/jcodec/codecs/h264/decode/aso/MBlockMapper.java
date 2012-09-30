package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents a spatial map of macroblocks belonging for one and the same slice
 * Needed for ASO (FMO).
 * 
 * @author Jay Codec
 * 
 */
public interface MBlockMapper {

    /**
     * @return Index of the macroblock right to the left of the current in this
     *         slice (the one that has common border on the left side). -1 if
     *         not available
     */
    int getLeftMBIdx(int mbIndex);

    /**
     * @return Index of the macroblock right on the top of the current in this
     *         slice (the one that has common border on the top side). -1 if not
     *         available.
     */
    int getTopMBIdx(int mbIndex);

    /**
     * @return Index of the macroblock right on the top and to the left of the
     *         current (the one that has common top-left corner). -1 if not
     *         available.
     */
    int getTopLeftMBIndex(int mbIndex);

    /**
     * @return Index of the macroblock right on the top and to the right of the
     *         current (the one that has common top-right corner). -1 if not
     *         available.
     */
    int getTopRightMBIndex(int mbIndex);

    int[] getAddresses(int count);
}
