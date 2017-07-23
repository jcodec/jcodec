package org.jcodec.player.filters;

import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.jcodec.common.io.NIOUtils;

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
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixers) {
                System.out.println("Found Mixer: " + mixerInfo);

                Mixer m = AudioSystem.getMixer(mixerInfo);

                for (Info li : m.getSourceLineInfo()) {
                    AudioFormat[] formats = ((DataLine.Info) li).getFormats();
                    for (AudioFormat audioFormat : formats) {
                        System.out.println(audioFormat);
                    }
                }
            }

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

    public void write(ByteBuffer sound) {
        int written;
        if (sound.hasArray()) {
            written = line.write(sound.array(), sound.arrayOffset() + sound.position(), sound.remaining());
        } else {
            byte[] array = NIOUtils.toArray(sound);
            written = line.write(array, 0, array.length);
        }
        sound.position(sound.position() + written);
    }
}