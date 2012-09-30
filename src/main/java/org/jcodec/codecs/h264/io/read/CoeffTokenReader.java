package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.readCE;

import java.io.IOException;

import org.jcodec.codecs.h264.io.BTree;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class CoeffTokenReader {
    private ChromaFormat chromaFormat;

    public CoeffTokenReader(ChromaFormat chromaFormat) {
        this.chromaFormat = chromaFormat;
    }

    static class Item {
        int trailingOnes;
        int totalCoeff;
        String str;

        public Item(int totalOnes, int totalCoeff, String str) {
            this.str = str;
            this.totalCoeff = totalCoeff;
            this.trailingOnes = totalOnes;
        }

        public String toString() {
            return "#c: " + totalCoeff + ", #t1: " + trailingOnes;
        }
    }

    static Item[] nc0_2 = new Item[] { new Item(0, 0, "1"), new Item(0, 1, "000101"), new Item(1, 1, "01"),
            new Item(0, 2, "00000111"), new Item(1, 2, "000100"), new Item(2, 2, "001"), new Item(0, 3, "000000111"),
            new Item(1, 3, "00000110"), new Item(2, 3, "0000101"), new Item(3, 3, "00011"),
            new Item(0, 4, "0000000111"), new Item(1, 4, "000000110"), new Item(2, 4, "00000101"),
            new Item(3, 4, "000011"), new Item(0, 5, "00000000111"), new Item(1, 5, "0000000110"),
            new Item(2, 5, "000000101"), new Item(3, 5, "0000100"), new Item(0, 6, "0000000001111"),
            new Item(1, 6, "00000000110"), new Item(2, 6, "0000000101"), new Item(3, 6, "00000100"),
            new Item(0, 7, "0000000001011"), new Item(1, 7, "0000000001110"), new Item(2, 7, "00000000101"),
            new Item(3, 7, "000000100"), new Item(0, 8, "0000000001000"), new Item(1, 8, "0000000001010"),
            new Item(2, 8, "0000000001101"), new Item(3, 8, "0000000100"), new Item(0, 9, "00000000001111"),
            new Item(1, 9, "00000000001110"), new Item(2, 9, "0000000001001"), new Item(3, 9, "00000000100"),
            new Item(0, 10, "00000000001011"), new Item(1, 10, "00000000001010"), new Item(2, 10, "00000000001101"),
            new Item(3, 10, "0000000001100"), new Item(0, 11, "000000000001111"), new Item(1, 11, "000000000001110"),
            new Item(2, 11, "00000000001001"), new Item(3, 11, "00000000001100"), new Item(0, 12, "000000000001011"),
            new Item(1, 12, "000000000001010"), new Item(2, 12, "000000000001101"), new Item(3, 12, "00000000001000"),
            new Item(0, 13, "0000000000001111"), new Item(1, 13, "000000000000001"),
            new Item(2, 13, "000000000001001"), new Item(3, 13, "000000000001100"),
            new Item(0, 14, "0000000000001011"), new Item(1, 14, "0000000000001110"),
            new Item(2, 14, "0000000000001101"), new Item(3, 14, "000000000001000"),
            new Item(0, 15, "0000000000000111"), new Item(1, 15, "0000000000001010"),
            new Item(2, 15, "0000000000001001"), new Item(3, 15, "0000000000001100"),
            new Item(0, 16, "0000000000000100"), new Item(1, 16, "0000000000000110"),
            new Item(2, 16, "0000000000000101"), new Item(3, 16, "0000000000001000") };

    static Item[] nc2_4 = new Item[] { new Item(0, 0, "11"), new Item(0, 1, "001011"), new Item(1, 1, "10"),
            new Item(0, 2, "000111"), new Item(1, 2, "00111"), new Item(2, 2, "011"), new Item(0, 3, "0000111"),
            new Item(1, 3, "001010"), new Item(2, 3, "001001"), new Item(3, 3, "0101"), new Item(0, 4, "00000111"),
            new Item(1, 4, "000110"), new Item(2, 4, "000101"), new Item(3, 4, "0100"), new Item(0, 5, "00000100"),
            new Item(1, 5, "0000110"), new Item(2, 5, "0000101"), new Item(3, 5, "00110"), new Item(0, 6, "000000111"),
            new Item(1, 6, "00000110"), new Item(2, 6, "00000101"), new Item(3, 6, "001000"),
            new Item(0, 7, "00000001111"), new Item(1, 7, "000000110"), new Item(2, 7, "000000101"),
            new Item(3, 7, "000100"), new Item(0, 8, "00000001011"), new Item(1, 8, "00000001110"),
            new Item(2, 8, "00000001101"), new Item(3, 8, "0000100"), new Item(0, 9, "000000001111"),
            new Item(1, 9, "00000001010"), new Item(2, 9, "00000001001"), new Item(3, 9, "000000100"),
            new Item(0, 10, "000000001011"), new Item(1, 10, "000000001110"), new Item(2, 10, "000000001101"),
            new Item(3, 10, "00000001100"), new Item(0, 11, "000000001000"), new Item(1, 11, "000000001010"),
            new Item(2, 11, "000000001001"), new Item(3, 11, "00000001000"), new Item(0, 12, "0000000001111"),
            new Item(1, 12, "0000000001110"), new Item(2, 12, "0000000001101"), new Item(3, 12, "000000001100"),
            new Item(0, 13, "0000000001011"), new Item(1, 13, "0000000001010"), new Item(2, 13, "0000000001001"),
            new Item(3, 13, "0000000001100"), new Item(0, 14, "0000000000111"), new Item(1, 14, "00000000001011"),
            new Item(2, 14, "0000000000110"), new Item(3, 14, "0000000001000"), new Item(0, 15, "00000000001001"),
            new Item(1, 15, "00000000001000"), new Item(2, 15, "00000000001010"), new Item(3, 15, "0000000000001"),
            new Item(0, 16, "00000000000111"), new Item(1, 16, "00000000000110"), new Item(2, 16, "00000000000101"),
            new Item(3, 16, "00000000000100") };

    static Item[] nc4_8 = new Item[] { new Item(0, 0, "1111"), new Item(0, 1, "001111"), new Item(1, 1, "1110"),
            new Item(0, 2, "001011"), new Item(1, 2, "01111"), new Item(2, 2, "1101"), new Item(0, 3, "001000"),
            new Item(1, 3, "01100"), new Item(2, 3, "01110"), new Item(3, 3, "1100"), new Item(0, 4, "0001111"),
            new Item(1, 4, "01010"), new Item(2, 4, "01011"), new Item(3, 4, "1011"), new Item(0, 5, "0001011"),
            new Item(1, 5, "01000"), new Item(2, 5, "01001"), new Item(3, 5, "1010"), new Item(0, 6, "0001001"),
            new Item(1, 6, "001110"), new Item(2, 6, "001101"), new Item(3, 6, "1001"), new Item(0, 7, "0001000"),
            new Item(1, 7, "001010"), new Item(2, 7, "001001"), new Item(3, 7, "1000"), new Item(0, 8, "00001111"),
            new Item(1, 8, "0001110"), new Item(2, 8, "0001101"), new Item(3, 8, "01101"), new Item(0, 9, "00001011"),
            new Item(1, 9, "00001110"), new Item(2, 9, "0001010"), new Item(3, 9, "001100"),
            new Item(0, 10, "000001111"), new Item(1, 10, "00001010"), new Item(2, 10, "00001101"),
            new Item(3, 10, "0001100"), new Item(0, 11, "000001011"), new Item(1, 11, "000001110"),
            new Item(2, 11, "00001001"), new Item(3, 11, "00001100"), new Item(0, 12, "000001000"),
            new Item(1, 12, "000001010"), new Item(2, 12, "000001101"), new Item(3, 12, "00001000"),
            new Item(0, 13, "0000001101"), new Item(1, 13, "000000111"), new Item(2, 13, "000001001"),
            new Item(3, 13, "000001100"), new Item(0, 14, "0000001001"), new Item(1, 14, "0000001100"),
            new Item(2, 14, "0000001011"), new Item(3, 14, "0000001010"), new Item(0, 15, "0000000101"),
            new Item(1, 15, "0000001000"), new Item(2, 15, "0000000111"), new Item(3, 15, "0000000110"),
            new Item(0, 16, "0000000001"), new Item(1, 16, "0000000100"), new Item(2, 16, "0000000011"),
            new Item(3, 16, "0000000010") };

    static Item[] nc8p = new Item[] { new Item(0, 0, "000011"), new Item(0, 1, "000000"), new Item(1, 1, "000001"),
            new Item(0, 2, "000100"), new Item(1, 2, "000101"), new Item(2, 2, "000110"), new Item(0, 3, "001000"),
            new Item(1, 3, "001001"), new Item(2, 3, "001010"), new Item(3, 3, "001011"), new Item(0, 4, "001100"),
            new Item(1, 4, "001101"), new Item(2, 4, "001110"), new Item(3, 4, "001111"), new Item(0, 5, "010000"),
            new Item(1, 5, "010001"), new Item(2, 5, "010010"), new Item(3, 5, "010011"), new Item(0, 6, "010100"),
            new Item(1, 6, "010101"), new Item(2, 6, "010110"), new Item(3, 6, "010111"), new Item(0, 7, "011000"),
            new Item(1, 7, "011001"), new Item(2, 7, "011010"), new Item(3, 7, "011011"), new Item(0, 8, "011100"),
            new Item(1, 8, "011101"), new Item(2, 8, "011110"), new Item(3, 8, "011111"), new Item(0, 9, "100000"),
            new Item(1, 9, "100001"), new Item(2, 9, "100010"), new Item(3, 9, "100011"), new Item(0, 10, "100100"),
            new Item(1, 10, "100101"), new Item(2, 10, "100110"), new Item(3, 10, "100111"), new Item(0, 11, "101000"),
            new Item(1, 11, "101001"), new Item(2, 11, "101010"), new Item(3, 11, "101011"), new Item(0, 12, "101100"),
            new Item(1, 12, "101101"), new Item(2, 12, "101110"), new Item(3, 12, "101111"), new Item(0, 13, "110000"),
            new Item(1, 13, "110001"), new Item(2, 13, "110010"), new Item(3, 13, "110011"), new Item(0, 14, "110100"),
            new Item(1, 14, "110101"), new Item(2, 14, "110110"), new Item(3, 14, "110111"), new Item(0, 15, "111000"),
            new Item(1, 15, "111001"), new Item(2, 15, "111010"), new Item(3, 15, "111011"), new Item(0, 16, "111100"),
            new Item(1, 16, "111101"), new Item(2, 16, "111110"), new Item(3, 16, "111111") };

    static Item[] ncm1 = new Item[] { new Item(0, 0, "01"), new Item(0, 1, "000111"), new Item(1, 1, "1"),
            new Item(0, 2, "000100"), new Item(1, 2, "000110"), new Item(2, 2, "001"), new Item(0, 3, "000011"),
            new Item(1, 3, "0000011"), new Item(2, 3, "0000010"), new Item(3, 3, "000101"), new Item(0, 4, "000010"),
            new Item(1, 4, "00000011"), new Item(2, 4, "00000010"), new Item(3, 4, "0000000"), };

    static Item[] ncm2 = new Item[] { new Item(0, 0, "1"), new Item(0, 1, "0001111"), new Item(1, 1, "01"),
            new Item(0, 2, "0001110"), new Item(1, 2, "0001101"), new Item(2, 2, "001"), new Item(0, 3, "000000111"),
            new Item(1, 3, "0001100"), new Item(2, 3, "0001011"), new Item(3, 3, "00001"), new Item(0, 4, "000000110"),
            new Item(1, 4, "000000101"), new Item(2, 4, "0001010"), new Item(3, 4, "000001"),
            new Item(0, 5, "0000000111"), new Item(1, 5, "0000000110"), new Item(2, 5, "000000100"),
            new Item(3, 5, "0001001"), new Item(0, 6, "00000000111"), new Item(1, 6, "00000000110"),
            new Item(2, 6, "0000000101"), new Item(3, 6, "0001000"), new Item(0, 7, "000000000111"),
            new Item(1, 7, "000000000110"), new Item(2, 7, "00000000101"), new Item(3, 7, "0000000100"),
            new Item(0, 8, "0000000000111"), new Item(1, 8, "000000000101"), new Item(2, 8, "000000000100"),
            new Item(3, 8, "00000000100") };

    static BTree nc0_2_bt = new BTree();
    static BTree nc2_4_bt = new BTree();
    static BTree nc4_8_bt = new BTree();
    static BTree nc8p_bt = new BTree();
    static BTree ncm1_bt = new BTree();
    static BTree ncm2_bt = new BTree();

    static {
        for (int i = 0; i < nc0_2.length; i++) {
            nc0_2_bt.addString(nc0_2[i].str, nc0_2[i]);
        }
        for (int i = 0; i < nc2_4.length; i++) {
            nc2_4_bt.addString(nc2_4[i].str, nc2_4[i]);
        }
        for (int i = 0; i < nc4_8.length; i++) {
            nc4_8_bt.addString(nc4_8[i].str, nc4_8[i]);
        }
        for (int i = 0; i < nc8p.length; i++) {
            nc8p_bt.addString(nc8p[i].str, nc8p[i]);
        }
        for (int i = 0; i < ncm1.length; i++) {
            ncm1_bt.addString(ncm1[i].str, ncm1[i]);
        }
        for (int i = 0; i < ncm2.length; i++) {
            ncm2_bt.addString(ncm2[i].str, ncm2[i]);
        }
    }

    /**
     * Reads coeff token for all blocks except chorma DC
     * 
     * @param reader
     * @param leftToken
     * @param topToken
     * @return
     * @throws IOException
     */
    public CoeffToken read(InBits reader, CoeffToken leftToken, CoeffToken topToken) throws IOException {

        int nC = estimateNumberOfCoeffs(leftToken, topToken);

        return read(reader, nC);
    }

    /**
     * Reads coeff token for chroma DC block
     * 
     * @param reader
     * @return
     * @throws IOException
     */
    public CoeffToken readForChromaDC(InBits reader) throws IOException {

        int nC = estimateNumberOfCoeffsForChromaDC();

        return read(reader, nC);
    }

    protected int estimateNumberOfCoeffs(CoeffToken leftToken, CoeffToken topToken) {

        int nA = leftToken != null ? leftToken.totalCoeff : 0;
        int nB = topToken != null ? topToken.totalCoeff : 0;

        if (leftToken != null && topToken != null)
            return (nA + nB + 1) >> 1;
        else
            return nA + nB;

    }

    protected int estimateNumberOfCoeffsForChromaDC() {
        if (chromaFormat == ChromaFormat.YUV_420) {
            return -1;
        } else if (chromaFormat == ChromaFormat.YUV_422) {
            return -2;
        } else if (chromaFormat == ChromaFormat.YUV_444) {
            return 0;
        } else {
            throw new RuntimeException("monochrome shouldn't be here");
        }
    }

    protected static CoeffToken read(InBits in, int nC) throws IOException {

        BTree bt = null;
        if (nC == -2) {
            bt = ncm2_bt;
        } else if (nC == -1) {
            bt = ncm1_bt;
        } else if (nC >= 0 && nC < 2) {
            bt = nc0_2_bt;
        } else if (nC >= 2 && nC < 4) {
            bt = nc2_4_bt;
        } else if (nC >= 4 && nC < 8) {
            bt = nc4_8_bt;
        } else if (nC >= 8) {
            bt = nc8p_bt;
        }
        if (bt != null) {
            Item i = (Item) readCE(in, bt, "Coeff token, nc = " + nC);
            if (i != null) {
                return new CoeffToken(i.totalCoeff, i.trailingOnes);
            }
        }
        return null;
    }

}