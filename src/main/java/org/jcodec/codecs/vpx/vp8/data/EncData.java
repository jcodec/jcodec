package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

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
public class EncData {
    public FullAccessIntArrPointer cx_data_dst_buf;
    public int cx_data_pad_before;
    public int cx_data_pad_after;
    public CodecPkt cx_data_pkt;
    int total_encoders = 1;
}