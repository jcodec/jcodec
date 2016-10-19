package org.jcodec.codecs.y4m;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.jcodec.common.model.Picture8Bit;

/**
 * Stores frames into Y4M file.
 * 
 * @author Stanislav Vitvitskiy
 * 
 */
public class Y4MEncoder {
    private WritableByteChannel ch;
    private boolean headerWritten;
    private int width;
    private int height;
    private byte[] scratchBuffer;
    public static final byte[] frameTag = "FRAME\n".getBytes();

    public Y4MEncoder(WritableByteChannel ch) {
        this.ch = ch;
    }

    public void encodeFrame(Picture8Bit picture) throws IOException {
        if (!headerWritten) {
            writeHeader(picture);
            headerWritten = true;
        }
        if (picture.getWidth() != width || picture.getHeight() != height) {
            throw new RuntimeException("Changing picture dimentsions is not allowed in Y4M.");
        }
        ch.write(ByteBuffer.wrap(frameTag));
        ch.write(shiftPlane(picture.getPlaneData(0)));
        ch.write(shiftPlane(picture.getPlaneData(1)));
        ch.write(shiftPlane(picture.getPlaneData(2)));
    }

    protected void writeHeader(Picture8Bit picture) throws IOException {
        byte[] bytes = String.format("YUV4MPEG2 W%d H%d F25:1 Ip A0:0 C420jpeg XYSCSS=420JPEG\n", picture.getWidth(),
                picture.getHeight()).getBytes();
        ch.write(ByteBuffer.wrap(bytes));
        this.width = picture.getWidth();
        this.height = picture.getHeight();
        this.scratchBuffer = new byte[width * height];
    }

    protected ByteBuffer shiftPlane(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            scratchBuffer[i] = (byte) (data[i] + 128);
        }
        return ByteBuffer.wrap(scratchBuffer, 0, data.length);
    }
}
