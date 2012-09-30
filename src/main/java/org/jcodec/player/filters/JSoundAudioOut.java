package org.jcodec.player.filters;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jcodec.common.io.Buffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JSoundAudioOut implements AudioOut {

    private SourceDataLine line;

    public void open(AudioFormat fmt, int frames) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Line matching " + info + " not supported.");
        }
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, frames * fmt.getFrameSize());
            line.start();
        } catch (LineUnavailableException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close() {
        line.stop();
        line.close();
    }

    public long playedMs() {
        return line.getMicrosecondPosition();
    }
    
    public long playedFrames() {
        return line.getLongFramePosition();
    }

    public int write(byte[] pkt, int off, int length) {
        return line.write(pkt, off, length);
    }

    public int available() {
        return line.available();
    }

    public int bufferSize() {
        return line.getBufferSize();
    }

    public void pause() {
        line.stop();
    }

    public void resume() {
        line.start();
    }

    public void flush() {
        line.flush();
    }

    public void drain() {
        line.drain();
    }

    public void write(Buffer sound) {
        write(sound.buffer, sound.pos, sound.remaining());
    }
}
