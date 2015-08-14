package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;

public interface IBitStream {

    void destroy();

    void setData(byte[] data);

    void byteAlign() throws AACException;

    void reset();

    int getPosition();

    int getBitsLeft();

    int readBits(int n) throws AACException;

    int readBit() throws AACException;

    boolean readBool() throws AACException;

    int peekBits(int n) throws AACException;

    int peekBit() throws AACException;

    void skipBits(int n) throws AACException;

    void skipBit() throws AACException;

    int maskBits(int n);

}