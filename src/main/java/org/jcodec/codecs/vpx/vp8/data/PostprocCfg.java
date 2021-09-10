package org.jcodec.codecs.vpx.vp8.data;

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
public class PostprocCfg {
    int post_proc_flag;
    int deblocking_level; /**< the strength of deblocking, valid range [0, 16] */
    int noise_level; /**< the strength of additive noise, valid range [0, 16] */
}
