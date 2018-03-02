package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Vector of 2 int16 elemente packed into a single int32 word
 * 
 * @author The JCodec project
 * 
 */
public class Vector2Int {
    public static int el16_0(int packed) {
        return (packed << 16) >> 16;
    }

    public static int el16_1(int packed) {
        return (packed >> 16);
    }
    
    public static int el16(int packed, int n) {
        switch (n) {
        case 0:
            return el16_0(packed);
        default:
            return el16_1(packed);
        }
    }
    
    public static int set16_0(int packed, int el) {
        return (packed & ~0xffff) | (el & 0xffff); 
    }
    
    public static int set16_1(int packed, int el) {
        return (packed & ~0xffff0000) | ((el & 0xffff) << 16); 
    }
    
    public static int set16(int packed, int el, int n) {
        switch (n) {
        case 0:
            return set16_0(packed, el);
        default:
            return set16_1(packed, el);
        }
    }

    public static int pack16(int el0, int el1) {
        return ((el1 & 0xffff) << 16) | (el0 & 0xffff); 
    }
}
