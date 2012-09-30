package org.jcodec.codecs.mjpeg;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class Markers {

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
    /** Define restart interval marker */
    public static final int DRI = 0xdd;

}
