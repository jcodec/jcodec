package org.jcodec.codecs.mjpeg;

import org.jcodec.common.io.VLC;
import org.jcodec.common.io.VLCBuilder;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class JpegConst {
    
    public final static int[] naturalOrder = new int[] { 0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12,
        19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23,
        30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63 };
    
    public final static VLC YDC_DEFAULT;
    public final static VLC YAC_DEFAULT;
    public final static VLC CDC_DEFAULT;
    public final static VLC CAC_DEFAULT;
    static {
        VLCBuilder bldr1 = new VLCBuilder();
        bldr1.set(0, "00");
        bldr1.set(1, "010");
        bldr1.set(2, "011");
        bldr1.set(3, "100");
        bldr1.set(4, "101");
        bldr1.set(5, "110");
        bldr1.set(6, "1110");
        bldr1.set(7, "11110");
        bldr1.set(8, "111110");
        bldr1.set(9, "1111110");
        bldr1.set(10, "11111110");
        bldr1.set(11, "111111110");
        YDC_DEFAULT = bldr1.getVLC();

        VLCBuilder bldr2 =  new VLCBuilder();
        bldr2.set(0, "00");
        bldr2.set(1, "01");
        bldr2.set(2, "10");
        bldr2.set(3, "110");
        bldr2.set(4, "1110");
        bldr2.set(5, "11110");
        bldr2.set(6, "111110");
        bldr2.set(7, "1111110");
        bldr2.set(8, "11111110");
        bldr2.set(9, "111111110");
        bldr2.set(10, "1111111110");
        bldr2.set(11, "11111111110");
        CDC_DEFAULT = bldr2.getVLC();

        VLCBuilder bldr3 =  new VLCBuilder();
        bldr3.set(0x00, "1010");
        bldr3.set(0x01, "00");
        bldr3.set(0x02, "01");
        bldr3.set(0x03, "100");
        bldr3.set(0x04, "1011");
        bldr3.set(0x05, "11010");
        bldr3.set(0x06, "1111000");
        bldr3.set(0x07, "11111000");
        bldr3.set(0x08, "1111110110");
        bldr3.set(0x09, "1111111110000010");
        bldr3.set(0x0A, "1111111110000011");
        bldr3.set(0x11, "1100");
        bldr3.set(0x12, "11011");
        bldr3.set(0x13, "1111001");
        bldr3.set(0x14, "111110110");
        bldr3.set(0x15, "11111110110");
        bldr3.set(0x16, "1111111110000100");
        bldr3.set(0x17, "1111111110000101");
        bldr3.set(0x18, "1111111110000110");
        bldr3.set(0x19, "1111111110000111");
        bldr3.set(0x1A, "1111111110001000");

        bldr3.set(0x21, "11100");
        bldr3.set(0x22, "11111001");
        bldr3.set(0x23, "1111110111");
        bldr3.set(0x24, "111111110100");
        bldr3.set(0x25, "1111111110001001");
        bldr3.set(0x26, "1111111110001010");
        bldr3.set(0x27, "1111111110001011");
        bldr3.set(0x28, "1111111110001100");
        bldr3.set(0x29, "1111111110001101");
        bldr3.set(0x2A, "1111111110001110");

        bldr3.set(0x31, "111010");
        bldr3.set(0x32, "111110111");
        bldr3.set(0x33, "111111110101");
        bldr3.set(0x34, "1111111110001111");
        bldr3.set(0x35, "1111111110010000");
        bldr3.set(0x36, "1111111110010001");
        bldr3.set(0x37, "1111111110010010");
        bldr3.set(0x38, "1111111110010011");
        bldr3.set(0x39, "1111111110010100");
        bldr3.set(0x3A, "1111111110010101");

        bldr3.set(0x41, "111011");
        bldr3.set(0x42, "1111111000");
        bldr3.set(0x43, "1111111110010110");
        bldr3.set(0x44, "1111111110010111");
        bldr3.set(0x45, "1111111110011000");
        bldr3.set(0x46, "1111111110011001");
        bldr3.set(0x47, "1111111110011010");
        bldr3.set(0x48, "1111111110011011");
        bldr3.set(0x49, "1111111110011100");
        bldr3.set(0x4A, "1111111110011101");

        bldr3.set(0x51, "1111010");
        bldr3.set(0x52, "11111110111");
        bldr3.set(0x53, "1111111110011110");
        bldr3.set(0x54, "1111111110011111");
        bldr3.set(0x55, "1111111110100000");
        bldr3.set(0x56, "1111111110100001");
        bldr3.set(0x57, "1111111110100010");
        bldr3.set(0x58, "1111111110100011");
        bldr3.set(0x59, "1111111110100100");
        bldr3.set(0x5A, "1111111110100101");

        bldr3.set(0x61, "1111011");
        bldr3.set(0x62, "111111110110");
        bldr3.set(0x63, "1111111110100110");
        bldr3.set(0x64, "1111111110100111");
        bldr3.set(0x65, "1111111110101000");
        bldr3.set(0x66, "1111111110101001");
        bldr3.set(0x67, "1111111110101010");
        bldr3.set(0x68, "1111111110101011");
        bldr3.set(0x69, "1111111110101100");
        bldr3.set(0x6A, "1111111110101101");

        bldr3.set(0x71, "11111010");
        bldr3.set(0x72, "111111110111");
        bldr3.set(0x73, "1111111110101110");
        bldr3.set(0x74, "1111111110101111");
        bldr3.set(0x75, "1111111110110000");
        bldr3.set(0x76, "1111111110110001");
        bldr3.set(0x77, "1111111110110010");
        bldr3.set(0x78, "1111111110110011");
        bldr3.set(0x79, "1111111110110100");
        bldr3.set(0x7A, "1111111110110101");

        bldr3.set(0x81, "111111000");
        bldr3.set(0x82, "111111111000000");
        bldr3.set(0x83, "1111111110110110");
        bldr3.set(0x84, "1111111110110111");
        bldr3.set(0x85, "1111111110111000");
        bldr3.set(0x86, "1111111110111001");
        bldr3.set(0x87, "1111111110111010");
        bldr3.set(0x88, "1111111110111011");
        bldr3.set(0x89, "1111111110111100");
        bldr3.set(0x8A, "1111111110111101");

        bldr3.set(0x91, "111111001");
        bldr3.set(0x92, "1111111110111110");
        bldr3.set(0x93, "1111111110111111");
        bldr3.set(0x94, "1111111111000000");
        bldr3.set(0x95, "1111111111000001");
        bldr3.set(0x96, "1111111111000010");
        bldr3.set(0x97, "1111111111000011");
        bldr3.set(0x98, "1111111111000100");
        bldr3.set(0x99, "1111111111000101");
        bldr3.set(0x9A, "1111111111000110");

        bldr3.set(0xA1, "111111010");
        bldr3.set(0xA2, "1111111111000111");
        bldr3.set(0xA3, "1111111111001000");
        bldr3.set(0xA4, "1111111111001001");
        bldr3.set(0xA5, "1111111111001010");
        bldr3.set(0xA6, "1111111111001011");
        bldr3.set(0xA7, "1111111111001100");
        bldr3.set(0xA8, "1111111111001101");
        bldr3.set(0xA9, "1111111111001110");
        bldr3.set(0xAA, "1111111111001111");

        bldr3.set(0xB1, "1111111001");
        bldr3.set(0xB2, "1111111111010000");
        bldr3.set(0xB3, "1111111111010001");
        bldr3.set(0xB4, "1111111111010010");
        bldr3.set(0xB5, "1111111111010011");
        bldr3.set(0xB6, "1111111111010100");
        bldr3.set(0xB7, "1111111111010101");
        bldr3.set(0xB8, "1111111111010110");
        bldr3.set(0xB9, "1111111111010111");
        bldr3.set(0xBA, "1111111111011000");

        bldr3.set(0xC1, "1111111010");
        bldr3.set(0xC2, "1111111111011001");
        bldr3.set(0xC3, "1111111111011010");
        bldr3.set(0xC4, "1111111111011011");
        bldr3.set(0xC5, "1111111111011100");
        bldr3.set(0xC6, "1111111111011101");
        bldr3.set(0xC7, "1111111111011110");
        bldr3.set(0xC8, "1111111111011111");
        bldr3.set(0xC9, "1111111111100000");
        bldr3.set(0xCA, "1111111111100001");

        bldr3.set(0xD1, "11111111000");
        bldr3.set(0xD2, "1111111111100010");
        bldr3.set(0xD3, "1111111111100011");
        bldr3.set(0xD4, "1111111111100100");
        bldr3.set(0xD5, "1111111111100101");
        bldr3.set(0xD6, "1111111111100110");
        bldr3.set(0xD7, "1111111111100111");
        bldr3.set(0xD8, "1111111111101000");
        bldr3.set(0xD9, "1111111111101001");
        bldr3.set(0xDA, "1111111111101010");

        bldr3.set(0xE1, "1111111111101011");
        bldr3.set(0xE2, "1111111111101100");
        bldr3.set(0xE3, "1111111111101101");
        bldr3.set(0xE4, "1111111111101110");
        bldr3.set(0xE5, "1111111111101111");
        bldr3.set(0xE6, "1111111111110000");
        bldr3.set(0xE7, "1111111111110001");
        bldr3.set(0xE8, "1111111111110010");
        bldr3.set(0xE9, "1111111111110011");
        bldr3.set(0xEA, "1111111111110100");

        bldr3.set(0xF0, "11111111001");
        bldr3.set(0xF1, "1111111111110101");
        bldr3.set(0xF2, "1111111111110110");
        bldr3.set(0xF3, "1111111111110111");
        bldr3.set(0xF4, "1111111111111000");
        bldr3.set(0xF5, "1111111111111001");
        bldr3.set(0xF6, "1111111111111010");
        bldr3.set(0xF7, "1111111111111011");
        bldr3.set(0xF8, "1111111111111100");
        bldr3.set(0xF9, "1111111111111101");
        bldr3.set(0xFA, "1111111111111110");
        YAC_DEFAULT = bldr3.getVLC();

        VLCBuilder bldr4 =  new VLCBuilder();
        bldr4.set(0x00, "00");
        bldr4.set(0x01, "01");
        bldr4.set(0x02, "100");
        bldr4.set(0x03, "1010");
        bldr4.set(0x04, "11000");
        bldr4.set(0x05, "11001");
        bldr4.set(0x06, "111000");
        bldr4.set(0x07, "1111000");
        bldr4.set(0x08, "111110100");
        bldr4.set(0x09, "1111110110");
        bldr4.set(0x0A, "111111110100");

        bldr4.set(0x11, "1011");
        bldr4.set(0x12, "111001");
        bldr4.set(0x13, "11110110");
        bldr4.set(0x14, "111110101");
        bldr4.set(0x15, "11111110110");
        bldr4.set(0x16, "111111110101");
        bldr4.set(0x17, "1111111110001000");
        bldr4.set(0x18, "1111111110001001");
        bldr4.set(0x19, "1111111110001010");
        bldr4.set(0x1A, "1111111110001011");

        bldr4.set(0x21, "11010");
        bldr4.set(0x22, "11110111");
        bldr4.set(0x23, "1111110111");
        bldr4.set(0x24, "111111110110");
        bldr4.set(0x25, "111111111000010");
        bldr4.set(0x26, "1111111110001100");
        bldr4.set(0x27, "1111111110001101");
        bldr4.set(0x28, "1111111110001110");
        bldr4.set(0x29, "1111111110001111");
        bldr4.set(0x2A, "1111111110010000");

        bldr4.set(0x31, "11011");
        bldr4.set(0x32, "11111000");
        bldr4.set(0x33, "1111111000");
        bldr4.set(0x34, "111111110111");
        bldr4.set(0x35, "1111111110010001");
        bldr4.set(0x36, "1111111110010010");
        bldr4.set(0x37, "1111111110010011");
        bldr4.set(0x38, "1111111110010100");
        bldr4.set(0x39, "1111111110010101");
        bldr4.set(0x3A, "1111111110010110");

        bldr4.set(0x41, "111010");
        bldr4.set(0x42, "111110110");
        bldr4.set(0x43, "1111111110010111");
        bldr4.set(0x44, "1111111110011000");
        bldr4.set(0x45, "1111111110011001");
        bldr4.set(0x46, "1111111110011010");
        bldr4.set(0x47, "1111111110011011");
        bldr4.set(0x48, "1111111110011100");
        bldr4.set(0x49, "1111111110011101");
        bldr4.set(0x4A, "1111111110011110");

        bldr4.set(0x51, "111011");
        bldr4.set(0x52, "1111111001");
        bldr4.set(0x53, "1111111110011111");
        bldr4.set(0x54, "1111111110100000");
        bldr4.set(0x55, "1111111110100001");
        bldr4.set(0x56, "1111111110100010");
        bldr4.set(0x57, "1111111110100011");
        bldr4.set(0x58, "1111111110100100");
        bldr4.set(0x59, "1111111110100101");
        bldr4.set(0x5A, "1111111110100110");

        bldr4.set(0x61, "1111001");
        bldr4.set(0x62, "11111110111");
        bldr4.set(0x63, "1111111110100111");
        bldr4.set(0x64, "1111111110101000");
        bldr4.set(0x65, "1111111110101001");
        bldr4.set(0x66, "1111111110101010");
        bldr4.set(0x67, "1111111110101011");
        bldr4.set(0x68, "1111111110101100");
        bldr4.set(0x69, "1111111110101101");
        bldr4.set(0x6A, "1111111110101110");

        bldr4.set(0x71, "1111010");
        bldr4.set(0x72, "11111111000");
        bldr4.set(0x73, "1111111110101111");
        bldr4.set(0x74, "1111111110110000");
        bldr4.set(0x75, "1111111110110001");
        bldr4.set(0x76, "1111111110110010");
        bldr4.set(0x77, "1111111110110011");
        bldr4.set(0x78, "1111111110110100");
        bldr4.set(0x79, "1111111110110101");
        bldr4.set(0x7A, "1111111110110110");

        bldr4.set(0x81, "11111001");
        bldr4.set(0x82, "1111111110110111");
        bldr4.set(0x83, "1111111110111000");
        bldr4.set(0x84, "1111111110111001");
        bldr4.set(0x85, "1111111110111010");
        bldr4.set(0x86, "1111111110111011");
        bldr4.set(0x87, "1111111110111100");
        bldr4.set(0x88, "1111111110111101");
        bldr4.set(0x89, "1111111110111110");
        bldr4.set(0x8A, "1111111110111111");

        bldr4.set(0x91, "111110111");
        bldr4.set(0x92, "1111111111000000");
        bldr4.set(0x93, "1111111111000001");
        bldr4.set(0x94, "1111111111000010");
        bldr4.set(0x95, "1111111111000011");
        bldr4.set(0x96, "1111111111000100");
        bldr4.set(0x97, "1111111111000101");
        bldr4.set(0x98, "1111111111000110");
        bldr4.set(0x99, "1111111111000111");
        bldr4.set(0x9A, "1111111111001000");

        bldr4.set(0xA1, "111111000");
        bldr4.set(0xA2, "1111111111001001");
        bldr4.set(0xA3, "1111111111001010");
        bldr4.set(0xA4, "1111111111001011");
        bldr4.set(0xA5, "1111111111001100");
        bldr4.set(0xA6, "1111111111001101");
        bldr4.set(0xA7, "1111111111001110");
        bldr4.set(0xA8, "1111111111001111");
        bldr4.set(0xA9, "1111111111010000");
        bldr4.set(0xAA, "1111111111010001");

        bldr4.set(0xB1, "111111001");
        bldr4.set(0xB2, "1111111111010010");
        bldr4.set(0xB3, "1111111111010011");
        bldr4.set(0xB4, "1111111111010100");
        bldr4.set(0xB5, "1111111111010101");
        bldr4.set(0xB6, "1111111111010110");
        bldr4.set(0xB7, "1111111111010111");
        bldr4.set(0xB8, "1111111111011000");
        bldr4.set(0xB9, "1111111111011001");
        bldr4.set(0xBA, "1111111111011010");

        bldr4.set(0xC1, "111111010");
        bldr4.set(0xC2, "1111111111011011");
        bldr4.set(0xC3, "1111111111011100");
        bldr4.set(0xC4, "1111111111011101");
        bldr4.set(0xC5, "1111111111011110");
        bldr4.set(0xC6, "1111111111011111");
        bldr4.set(0xC7, "1111111111100000");
        bldr4.set(0xC8, "1111111111100001");
        bldr4.set(0xC9, "1111111111100010");
        bldr4.set(0xCA, "1111111111100011");
        bldr4.set(0xD1, "11111111001");
        bldr4.set(0xD2, "1111111111100100");
        bldr4.set(0xD3, "1111111111100101");
        bldr4.set(0xD4, "1111111111100110");
        bldr4.set(0xD5, "1111111111100111");
        bldr4.set(0xD6, "1111111111101000");
        bldr4.set(0xD7, "1111111111101001");
        bldr4.set(0xD8, "1111111111101010");
        bldr4.set(0xD9, "1111111111101011");
        bldr4.set(0xDA, "1111111111101100");
        bldr4.set(0xE1, "11111111100000");
        bldr4.set(0xE2, "1111111111101101");
        bldr4.set(0xE3, "1111111111101110");
        bldr4.set(0xE4, "1111111111101111");
        bldr4.set(0xE5, "1111111111110000");
        bldr4.set(0xE6, "1111111111110001");
        bldr4.set(0xE7, "1111111111110010");
        bldr4.set(0xE8, "1111111111110011");
        bldr4.set(0xE9, "1111111111110100");
        bldr4.set(0xEA, "1111111111110101");
        bldr4.set(0xF0, "1111111010");
        bldr4.set(0xF1, "111111111000011");
        bldr4.set(0xF2, "1111111111110110");
        bldr4.set(0xF3, "1111111111110111");
        bldr4.set(0xF4, "1111111111111000");
        bldr4.set(0xF5, "1111111111111001");
        bldr4.set(0xF6, "1111111111111010");
        bldr4.set(0xF7, "1111111111111011");
        bldr4.set(0xF8, "1111111111111100");
        bldr4.set(0xF9, "1111111111111101");
        bldr4.set(0xFA, "1111111111111110");
        CAC_DEFAULT = bldr4.getVLC();
    }
    
    private final static String[] names = new String[256];
    static {
        for (int i = 0; i < names.length; i++) {
            names[i] = "(0x" + Integer.toHexString(i) + ")";
        }
        names[0xc0] = "SOF0";
        names[0xc1] = "SOF1";
        names[0xc2] = "SOF2";
        names[0xc3] = "SOF3";
        names[0xc4] = "DHT";
        names[0xdb] = "DQT";
        names[0xda] = "SOS";
        names[0xd9] = "EOI";
        names[0xd8] = "SOI";
        names[0xe0] = "APP0";
        names[0xe1] = "APP1";
        names[0xe2] = "APP2";
        names[0xe3] = "APP3";
        names[0xe4] = "APP4";
        names[0xe5] = "APP5";
        names[0xe6] = "APP6";
        names[0xe7] = "APP7";
        names[0xe8] = "APP8";
        names[0xe9] = "APP9";
        names[0xea] = "APPA";
        names[0xeb] = "APPB";
        names[0xec] = "APPC";
        names[0xed] = "APPD";
        names[0xee] = "APPE";
        names[0xef] = "APPF";
        names[0xd0] = "RST0";
        names[0xd1] = "RST1";
        names[0xd2] = "RST2";
        names[0xd3] = "RST3";
        names[0xd4] = "RST4";
        names[0xd5] = "RST5";
        names[0xd6] = "RST6";
        names[0xd7] = "RST7";
        names[0xdd] = "DRI";
    }

    public static String toString(int marker) {
        return names[marker];
    }

    /** Start Of Frame markers, non-differential, Huffman coding */
    /** Start Of Frame - Baseline DCT */
    public static final int SOF0 = 0xc0;
    /** Start Of Frame - Extended sequential DCT */
    public static final int SOF1 = 0xc1;
    /** Start Of Frame - Progressive DCT */
    public static final int SOF2 = 0xc2;
    /** Start Of Frame - Lossless (sequential) */
    public static final int SOF3 = 0xc3;
    /** Huffman table specification - Define Huffman table(s) */
    public static final int DHT = 0xc4;
    /** Define quantization table(s) */
    public static final int DQT = 0xdb;
    /** Start of scan */
    public static final int SOS = 0xda;
    /** End of image - standalone marker */
    public static final int EOI = 0xd9;
    /** Start of image - standalone marker */
    public static final int SOI = 0xd8;
    /** Reserved for application segments */
    public static final int APP0 = 0xe0;
    /** Reserved for application segments */
    public static final int APP1 = 0xe1;
    /** Reserved for application segments */
    public static final int APP2 = 0xe2;
    /** Reserved for application segments */
    public static final int APP3 = 0xe3;
    /** Reserved for application segments */
    public static final int APP4 = 0xe4;
    /** Reserved for application segments */
    public static final int APP5 = 0xe5;
    /** Reserved for application segments */
    public static final int APP6 = 0xe6;
    /** Reserved for application segments */
    public static final int APP7 = 0xe7;
    /** Reserved for application segments */
    public static final int APP8 = 0xe8;
    /** Reserved for application segments */
    public static final int APP9 = 0xe9;
    /** Reserved for application segments */
    public static final int APPA = 0xea;
    /** Reserved for application segments */
    public static final int APPB = 0xeb;
    /** Reserved for application segments */
    public static final int APPC = 0xec;
    /** Reserved for application segments */
    public static final int APPD = 0xed;
    /** Reserved for application segments */
    public static final int APPE = 0xee;
    /** Reserved for application segments */
    public static final int APPF = 0xef;
    /** Restart with modulo 8 count 0 */
    public static final int RST0 = 0xd0;
    /** Restart with modulo 8 count 1 */
    public static final int RST1 = 0xd1;
    /** Restart with modulo 8 count 2 */
    public static final int RST2 = 0xd2;
    /** Restart with modulo 8 count 3 */
    public static final int RST3 = 0xd3;
    /** Restart with modulo 8 count 4 */
    public static final int RST4 = 0xd4;
    /** Restart with modulo 8 count 5 */
    public static final int RST5 = 0xd5;
    /** Restart with modulo 8 count 6 */
    public static final int RST6 = 0xd6;
    /** Restart with modulo 8 count 7 */
    public static final int RST7 = 0xd7;
    
    public static final int COM = 0xfe;
    /** Define restart interval marker */
    public static final int DRI = 0xdd;
    
    
    public static int[] DEFAULT_QUANT_LUMA = { 16, 11, 12, 14, 12, 10, 16, 14, 13, 14, 18, 17, 16, 19, 24, 40, 26, 24,
            22, 22, 24, 49, 36, 37, 29, 40, 58, 51, 61, 60, 57, 51, 56, 55, 64, 72, 92, 78, 64, 68, 87, 69, 55, 56, 80,
            109, 81, 87, 95, 62, 103, 104, 103, 98, 77, 113, 121, 112, 100, 120, 92, 101, 103, 99 };
    public static int[] DEFAULT_QUANT_CHROMA = { 17, 18, 18, 24, 21, 24, 47, 26, 26, 47, 99, 66, 56, 66, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99 };
}
