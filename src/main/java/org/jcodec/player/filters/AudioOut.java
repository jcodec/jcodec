package org.jcodec.player.filters;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioOut {
    void open(AudioFormat fmt, int frames);

    void close();

    long playedMs();
    
    long playedFrames();

    int write(byte[] pkt, int i, int length);

    int available();
    
    int bufferSize();

    void pause();

    void resume();

    void flush();

    void drain();

    void write(Buffer sound);
}
