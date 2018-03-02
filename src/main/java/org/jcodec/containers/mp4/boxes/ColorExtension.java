package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.JCodecUtil2.asciiString;

import java.nio.ByteBuffer;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Default box factory
 * 
 * @author The JCodec project
 * 
 */
public class ColorExtension extends Box {

    private short primariesIndex;
    private short transferFunctionIndex;
    private short matrixIndex;
    private String type = "nclc";

    final static byte RANGE_UNSPECIFIED = 0;
    final static byte AVCOL_RANGE_MPEG = 1; ///< the normal 219*2^(n-8) "MPEG" YUV ranges
    final static byte AVCOL_RANGE_JPEG = 2; ///< the normal     2^n-1   "JPEG" YUV ranges
    private Byte colorRange = null;

    public ColorExtension(Header header) {
        super(header);
    }

    public void setColorRange(Byte colorRange) {
        this.colorRange = colorRange;
    }

    @Override
    public void parse(ByteBuffer input) {
        byte[] dst = new byte[4];
        input.get(dst);
        this.type = Platform.stringFromBytes(dst);
        primariesIndex = input.getShort();
        transferFunctionIndex = input.getShort();
        matrixIndex = input.getShort();
        if (input.hasRemaining()) {
            colorRange = input.get();
        }
    }

    @Override
    public void doWrite(ByteBuffer out) {
        out.put(asciiString(type));
        out.putShort(primariesIndex);
        out.putShort(transferFunctionIndex);
        out.putShort(matrixIndex);
        if (colorRange != null) {
            out.put(colorRange);
        }
    }
    
    @Override
    public int estimateSize() {
        return 8 + 8;
    }

    public static String fourcc() {
        return "colr";
    }

    public static ColorExtension createColorExtension(short primariesIndex, short transferFunctionIndex,
            short matrixIndex) {
        ColorExtension c = new ColorExtension(new Header(fourcc()));
        c.primariesIndex = primariesIndex;
        c.transferFunctionIndex = transferFunctionIndex;
        c.matrixIndex = matrixIndex;
        return c;
    }

    public static ColorExtension createColr() {
        return new ColorExtension(new Header(fourcc()));
    }

    public short getPrimariesIndex() {
        return primariesIndex;
    }

    public short getTransferFunctionIndex() {
        return transferFunctionIndex;
    }

    public short getMatrixIndex() {
        return matrixIndex;
    }
}
