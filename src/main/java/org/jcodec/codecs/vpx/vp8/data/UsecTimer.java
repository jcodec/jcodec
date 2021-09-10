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
public class UsecTimer {
    long begin, end;

    public void timerStart() {
        begin = System.nanoTime();
    }

    public void mark() {
        end = System.nanoTime();
    }

    public long elapsed() {
        return (end - begin) / 1000;
    }

}
