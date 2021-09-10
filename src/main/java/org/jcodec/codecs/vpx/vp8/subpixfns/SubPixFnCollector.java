package org.jcodec.codecs.vpx.vp8.subpixfns;

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
public interface SubPixFnCollector {
    public SubpixFN get4x4();

    public SubpixFN get8x4();

    public SubpixFN get8x8();

    public SubpixFN get16x16();

}
