package org.jcodec.codecs.png;

import org.jcodec.common.model.ColorSpace;

import java.nio.ByteBuffer;

class IHDR {
    static final int PNG_COLOR_MASK_ALPHA = 4;
    static final int PNG_COLOR_MASK_COLOR = 2;
    static final int PNG_COLOR_MASK_PALETTE = 1;
    int width;
    int height;
    byte bitDepth;
    byte colorType;
    private byte compressionType;
    private byte filterType;
    byte interlaceType;

    void write(ByteBuffer data) {
        data.putInt(width);
        data.putInt(height);
        data.put(bitDepth);
        data.put(colorType);
        data.put(compressionType);
        data.put(filterType);
        data.put(interlaceType);
    }

    void parse(ByteBuffer data) {
        width = data.getInt();
        height = data.getInt();
        bitDepth = data.get();
        colorType = data.get();
        compressionType = data.get();
        filterType = data.get();
        interlaceType = data.get();
        data.getInt();
    }

    int rowSize() {
        return (width * getBitsPerPixel() + 7) >> 3;
    }

    private int getNBChannels() {
        int channels;
        channels = 1;
        if ((colorType & (PNG_COLOR_MASK_COLOR | PNG_COLOR_MASK_PALETTE)) == PNG_COLOR_MASK_COLOR)
            channels = 3;
        if ((colorType & PNG_COLOR_MASK_ALPHA) != 0)
            channels++;
        return channels;
    }

    int getBitsPerPixel() {
        return bitDepth * getNBChannels();
    }

    ColorSpace colorSpace() {
        return ColorSpace.RGB;
    }
}
