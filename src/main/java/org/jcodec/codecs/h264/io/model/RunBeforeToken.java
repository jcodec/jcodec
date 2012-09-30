package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readCE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.BTree;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * run_before_token
 * 
 * 
 * @author Jay Codec
 * 
 */
public class RunBeforeToken {

    static String[][] codes = {
            { "1", "0" },
            { "1", "01", "00" },
            { "11", "10", "01", "00" },
            { "11", "10", "01", "001", "000" },
            { "11", "10", "011", "010", "001", "000" },
            { "11", "000", "001", "011", "010", "101", "100" },

            { "111", "110", "101", "100", "011", "010", "001", "0001", "00001",
                    "000001", "0000001", "00000001", "000000001", "0000000001",
                    "00000000001" } };
    static BTree[] btrees = new BTree[7];

    static {
        for (int i = 0; i < 7; i++) {
            btrees[i] = new BTree();
            for (int j = 0; j < codes[i].length; j++) {
                btrees[i].addString(codes[i][j], new Integer(j));
            }
        }
    }

    public static int read(InBits in, int zeros_left)
            throws IOException {
        if (zeros_left > 7)
            zeros_left = 7;
        Object value = readCE(in, btrees[zeros_left - 1], "Luma run");
        int intValue = ((Integer) value).intValue();
        return intValue;
    }
}