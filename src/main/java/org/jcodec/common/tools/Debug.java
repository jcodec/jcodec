package org.jcodec.common.tools;

import java.nio.ShortBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class Debug {
    public static void print8x8(int[] output) {
        int i = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", output[i]);
                i++;
            }
            System.out.println();
        }
    }

    public static void print8x8(short[] output) {
        int i = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", output[i]);
                i++;
            }
            System.out.println();
        }
    }

    public static void print8x8(ShortBuffer output) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", output.get());
            }
            System.out.println();
        }
    }

    public static void print(short[] table) {
        int i = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", table[i]);
                i++;
            }
            System.out.println();
        }
    }

    public static void trace(String format, Object... args) {
        if (debug)
            System.out.printf(format + ": %d\n", args);
    }

    public final static boolean debug = false;

    public static void print(int i) {
        if (debug)
            System.out.print(i);
    }

    public static void print(String string) {
        if (debug)
            System.out.print(string);
    }

    public static void println(String string) {
        if (debug)
            System.out.println(string);
    }
}
