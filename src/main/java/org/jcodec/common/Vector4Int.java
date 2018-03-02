package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Vector of 4 int8 elemente packed into a single int32 word
 * 
 * @author The JCodec project
 * 
 */
public class Vector4Int {
    public static int el8_0(int packed) {
        return (packed << 24) >> 24;
    }

    public static int el8_1(int packed) {
        return ((packed << 16) >> 24);
    }
    
    public static int el8_2(int packed) {
        return (packed << 8) >> 24;
    }

    public static int el8_3(int packed) {
        return (packed >> 24);
    }
    
    public static int el8(int packed, int n) {
        switch (n) {
        case 0:
            return el8_0(packed);
        case 1:
            return el8_1(packed);
        case 2:
            return el8_2(packed);
        default:
            return el8_3(packed);
        }
    }
    
    public static int set8_0(int packed, int el) {
        return (packed & ~0xff) | (el & 0xff); 
    }
    
    public static int set8_1(int packed, int el) {
        return (packed & ~0xff00) | ((el & 0xff) << 8); 
    }
    
    public static int set8_2(int packed, int el) {
        return (packed & ~0xff0000) | ((el & 0xff) << 16); 
    }
    
    public static int set8_3(int packed, int el) {
        return (packed & ~0xff0000) | ((el & 0xff) << 24); 
    }
    
    public static int set8(int packed, int el, int n) {
        switch (n) {
        case 0:
            return set8_0(packed, el);
        case 1:
            return set8_1(packed, el);
        case 2:
            return set8_2(packed, el);
        default:
            return set8_3(packed, el);
        }
    }

    public static int pack8(int el0, int el1, int el2, int el3) {
        return ((el3 & 0xff) << 24) | ((el2 & 0xff) << 16) | ((el1 & 0xff) << 8) | (el0 & 0xff); 
    }
}
