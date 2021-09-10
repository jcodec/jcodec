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
public class PostprocState {
    int last_q;
    int last_noise;
    int last_base_qindex;
    int last_frame_valid;
    int clamp;
    byte[] generated_noise;
}
