package org.jcodec.codecs.vpx.vp8.enums;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public enum ThrModes {
    THR_ZERO1, THR_DC, THR_NEAREST1, THR_NEAR1, THR_ZERO2, THR_NEAREST2, THR_ZERO3, THR_NEAREST3, THR_NEAR2, THR_NEAR3,
    THR_V_PRED, THR_H_PRED, THR_TM, THR_NEW1, THR_NEW2, THR_NEW3, THR_SPLIT1, THR_SPLIT2, THR_SPLIT3, THR_B_PRED
}
