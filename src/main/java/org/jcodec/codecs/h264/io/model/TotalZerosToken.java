package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readCE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.BTree;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * total_zeroz_token
 * 
 * @author Jay Codec
 * 
 */
public class TotalZerosToken {

    static String codes4x4[][] = {

            { "1", "011", "010", "0011", "0010", "00011", "00010", "000011", "000010", "0000011", "0000010",
                    "00000011", "00000010", "000000011", "000000010", "000000001" },

            { "111", "110", "101", "100", "011", "0101", "0100", "0011", "0010", "00011", "00010", "000011", "000010",
                    "000001", "000000" },

            { "0101", "111", "110", "101", "0100", "0011", "100", "011", "0010", "00011", "00010", "000001", "00001",
                    "000000" },

            { "00011", "111", "0101", "0100", "110", "101", "100", "0011", "011", "0010", "00010", "00001", "00000" },

            { "0101", "0100", "0011", "111", "110", "101", "100", "011", "0010", "00001", "0001", "00000" },

            { "000001", "00001", "111", "110", "101", "100", "011", "010", "0001", "001", "000000" },

            { "000001", "00001", "101", "100", "011", "11", "010", "0001", "001", "000000" },

            { "000001", "0001", "00001", "011", "11", "10", "010", "001", "000000" },

            { "000001", "000000", "0001", "11", "10", "001", "01", "00001" },

            { "00001", "00000", "001", "11", "10", "01", "0001" },

            { "0000", "0001", "001", "010", "1", "011" },

            { "0000", "0001", "01", "1", "001" },

            { "000", "001", "1", "01" },

            { "00", "01", "1" },

            { "0", "1" } };

    static String codes_cr_2x2[][] = { { "1", "01", "001", "000" },

    { "1", "01", "00" },

    { "1", "0" } };

    static String codes_cr_4x2[][] = { { "1", "010", "011", "0010", "0011", "0001", "00001", "00000" },

    { "000", "01", "001", "100", "101", "110", "111" },

    { "000", "001", "01", "10", "110", "111" },

    { "110", "00", "01", "10", "111" },

    { "00", "01", "10", "11" },

    { "00", "01", "1" },

    { "0", "1" } };

    static BTree[] btrees = new BTree[15];
    static BTree[] btrees2x2 = new BTree[3];
    static BTree[] btrees4x2 = new BTree[7];

    static {
        for (int i = 0; i < 15; i++) {
            btrees[i] = new BTree();
            for (int j = 0; j < codes4x4[i].length; j++) {
                btrees[i].addString(codes4x4[i][j], new Integer(j));
            }
        }

        for (int i = 0; i < 3; i++) {
            btrees2x2[i] = new BTree();
            for (int j = 0; j < codes_cr_2x2[i].length; j++) {
                btrees2x2[i].addString(codes_cr_2x2[i][j], new Integer(j));
            }
        }

        for (int i = 0; i < 7; i++) {
            btrees4x2[i] = new BTree();
            for (int j = 0; j < codes_cr_4x2[i].length; j++) {
                btrees4x2[i].addString(codes_cr_4x2[i][j], new Integer(j));
            }
        }
    }

    public static int read4x4(InBits in, int totalCoeff) throws IOException {
        Object value = readCE(in, btrees[totalCoeff - 1], "Zeros left");
        int intValue = ((Integer) value).intValue();
        return intValue;
    }

    public static int readCr2x2(InBits in, int totalCoeff) throws IOException {
        Object value = readCE(in, btrees2x2[totalCoeff - 1], "Zeros left");
        return ((Integer) value).intValue();
    }

    public static int readCr4x2(InBits in, int totalCoeff) throws IOException {
        Object value = readCE(in, btrees4x2[totalCoeff - 1], "Zeros left");
        return ((Integer) value).intValue();
    }
}