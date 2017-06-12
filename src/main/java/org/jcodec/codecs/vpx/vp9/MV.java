package org.jcodec.codecs.vpx.vp9;

/**
 * Contains functions to manager a 31 bit packed mv
 */
public class MV {
    
    public static int create(int x, int y, int ref) {
        return (ref << 28) | ((y & 0x3fff) << 14) | (x & 0x3fff);
    }
    
    public static int x(int mv) {
        return (mv << 18) >> 18;
    }
    
    public static int y(int mv) {
        return (mv << 4) >> 18;
    }
    
    public static int ref(int mv) {
        return (mv >> 28) & 0x3;
    }
}
