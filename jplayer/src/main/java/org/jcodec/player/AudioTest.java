package org.jcodec.player;

import static org.jcodec.common.SoundUtil.toJavax;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.SoundUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.player.filters.AudioOut;
import org.jcodec.player.filters.JSoundAudioOut;
import org.jcodec.player.filters.MediaInfo.AudioInfo;
import org.jcodec.player.filters.audio.AudioSource;
import org.jcodec.player.filters.audio.ToneAudioSource;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * AV Player
 * 
 */
public class AudioTest {
    public static void main(String[] args) throws IOException, InterruptedException, LineUnavailableException {

        // AudioSource tone = new WavAudioSource(new
        // RandomAccessFileInputStream(new File(args[0])));
        AudioSource tone = new ToneAudioSource();
        AudioOut qqq = new JSoundAudioOut();
        // int bufferSize = Player.AUDIO_PACKET_SIZE * Player.PACKETS_IN_BUFFER
        // * tone.getAudioFormat().getFrameSize();
        AudioInfo audioInfo = tone.getAudioInfo();
        AudioFormat af = audioInfo.getFormat();
        ByteBuffer pkt = ByteBuffer.allocate(af.getFrameSize() * 96000);
        qqq.open(toJavax(af), 1024 * Player.PACKETS_IN_BUFFER);
        // int i = 0;
        // System.gc();
        while (true) {
            // int thisTime = qqq.available() /
            // tone.getAudioFormat().getFrameSize();
            tone.getFrame(pkt);
            // if (i % 100 == 99) {
            // SoundFilter.out(line.getFormat(), pkt, SoundFilter.sine);
            // line.write(pkt, 0, pkt.length);
            // line.drain();
            // line.stop();
            // Thread.sleep(1000);
            // tone.seek(0, 1);
            // pkt = tone.getFrames(Player.AUDIO_PACKET_SIZE * 4);
            // line.start();
            // }
            // qqq.write(pkt, 0, thisTime *
            // tone.getAudioFormat().getFrameSize());
            // while (qqq.available() < (qqq.bufferSize() / 2))
            // Thread.sleep(1);
            qqq.write(pkt);
            // i++;
        }
        // qqq.close();
    }

    public static void main1(String[] args) throws IOException, InterruptedException, LineUnavailableException {

        // AudioSource tone = new WavAudioSource(new
        // RandomAccessFileInputStream(new File(args[0])));
        AudioSource tone = new ToneAudioSource();
        AudioInfo audioInfo = tone.getAudioInfo();
        AudioFormat af = audioInfo.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, toJavax(tone.getAudioInfo().getFormat()));
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Line matching " + info + " not supported.");
        }
        Clip clip = AudioSystem.getClip();
        ByteBuffer bb = ByteBuffer.allocate(af.getFrameSize() * 96000);
        tone.getFrame(bb);
        byte[] array = NIOUtils.toArray(bb);
        clip.open(toJavax(af), array, 0, array.length);
        clip.start();
        clip.drain();

        clip.close();
    }
}
