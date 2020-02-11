package net.sourceforge.jaad.aac.huffman;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public interface HCB {

    int ZERO_HCB = 0;
    int ESCAPE_HCB = 11;
    int NOISE_HCB = 13;
    int INTENSITY_HCB2 = 14;
    int INTENSITY_HCB = 15;
    //
    int FIRST_PAIR_HCB = 5;
}
