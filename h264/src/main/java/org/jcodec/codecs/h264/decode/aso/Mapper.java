package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface Mapper {
    boolean leftAvailable(int index);

    boolean topAvailable(int index);

    int getAddress(int index);

    int getMbX(int mbIndex);

    int getMbY(int mbIndex);

    boolean topRightAvailable(int mbIndex);

    boolean topLeftAvailable(int mbIdx);
}
