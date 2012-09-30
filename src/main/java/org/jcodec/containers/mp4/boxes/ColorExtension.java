package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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
    private final String type = "nclc";

    public ColorExtension(short primariesIndex, short transferFunctionIndex, short matrixIndex) {
        this();
        this.primariesIndex = primariesIndex;
        this.transferFunctionIndex = transferFunctionIndex;
        this.matrixIndex = matrixIndex;
    }

    public ColorExtension() {
        super(new Header(fourcc()));
    }

    @Override
    public void parse(InputStream input) throws IOException {
        long type = ReaderBE.readInt32(input);
        primariesIndex = (short) ReaderBE.readInt16(input);
        transferFunctionIndex = (short) ReaderBE.readInt16(input);
        matrixIndex = (short) ReaderBE.readInt16(input);
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        out.write(type.getBytes());
        out.writeShort(primariesIndex);
        out.writeShort(transferFunctionIndex);
        out.writeShort(matrixIndex);
    }

    public static String fourcc() {
        return "colr";
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
