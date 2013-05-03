package org.jcodec.codecs.h264.decode.aso;


public interface Mapper {
    boolean leftAvailable(int index);

    boolean topAvailable(int index);

    int getAddress(int index);

    int getMbX(int mbIndex);

    int getMbY(int mbIndex);

    boolean topRightAvailable(int mbIndex);

    boolean topLeftAvailable(int mbIdx);
}
