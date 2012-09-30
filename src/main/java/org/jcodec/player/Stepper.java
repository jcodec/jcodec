package org.jcodec.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;
import org.jcodec.player.filters.AudioOut;
import org.jcodec.player.filters.AudioSource;
import org.jcodec.player.filters.ChannelSelector;
import org.jcodec.player.filters.Resampler24To16;
import org.jcodec.player.filters.MediaInfo.AudioInfo;
import org.jcodec.player.filters.MediaInfo.VideoInfo;
import org.jcodec.player.filters.VideoOutput;
import org.jcodec.player.filters.VideoSource;
import org.jcodec.scale.ColorUtil;

import ch.lambdaj.Lambda;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Media step engine
 * 
 * @author The JCodec project
 * 
 */
public class Stepper {

    public static final int PACKETS_IN_BUFFER = 8;
    public static final int MAX_FRAMES = 10;

    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoOutput vo;
    private AudioOut ao;
    private VideoInfo mi;
    private AudioInfo ai;
    private Picture dst;

    private Object seekLock = new Object();
    private List<Frame> video;

    private RationalLarge pts;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Stepper(VideoSource videoSource, AudioSource audioSource, VideoOutput vo, AudioOut ao) {
        this.videoSource = videoSource;
        this.vo = vo;
        this.ao = ao;
        this.mi = videoSource.getMediaInfo();

        this.audioSource = insertResampler(audioSource);
        this.ai = this.audioSource.getAudioInfo();

        AudioInfo ai = this.audioSource.getAudioInfo();
        ao.open(ai.getFormat(), ai.getFramesPerPacket() * PACKETS_IN_BUFFER);

        try {
            video = new ArrayList<Frame>();
            for (int i = 0; i < MAX_FRAMES; i++)
                video.add(videoSource.decode(createTarget()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        next();
    }

    private AudioSource insertResampler(AudioSource src) {
        AudioFormat format = src.getAudioInfo().getFormat();

        AudioSource result = src;

        if (format.getSampleSizeInBits() == 24) {
            result = new Resampler24To16(result);
        }

        if (format.getChannels() > 2) {
            result = new ChannelSelector(result);
        }
        return result;
    }

    private int[][] createTarget() {
        Size dim = mi.getDim();
        int sz = 2 * dim.getWidth() * dim.getHeight();
        return new int[][] { new int[sz], new int[sz], new int[sz] };
    }

    public void seek(RationalLarge where) {
        try {
            if (!videoSource.seek(where.getNum(), where.getDen()))
                return;

            video = new ArrayList<Frame>();
            while (video.size() < MAX_FRAMES) {
                Frame frame = videoSource.decode(createTarget());
                if (frame == null)
                    break;
                RationalLarge framePts = frame.getPts();
                if (framePts.getNum() * where.getDen() >= where.getNum() * framePts.getDen())
                    video.add(frame);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        next();
    }

    private void nextInt() throws IOException {
        synchronized (seekLock) {
            video = Lambda.sort(video, Lambda.on(Frame.class).getPts().getNum());
            Frame frame = video.remove(0);
            show(frame);
            pts = frame.getPts();
            audioSource.seek(pts.getNum(), pts.getDen());
            frame = videoSource.decode(frame.getPic().getData());
            if (frame != null)
                video.add(frame);

            playSound(150);
        }
    }

    private void show(Frame frame) {
        Picture src = frame.getPic();
        if (src.getColor() != vo.getColorSpace()) {
            if (dst == null || dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight())
                dst = Picture.create(src.getWidth(), src.getHeight(), vo.getColorSpace());

            ColorUtil.getTransform(src.getColor(), vo.getColorSpace()).transform(src, dst);

            vo.show(dst, frame.getPixelAspect());
        } else {
            vo.show(src, frame.getPixelAspect());
        }
    }

    private void playSound(int ms) throws IOException {
        AudioFormat af = ai.getFormat();
        Buffer sound = new Buffer((int) (ms * af.getFrameRate() / 1000) * af.getFrameSize()), fork = sound.fork();
        byte[] buf = new byte[ai.getFramesPerPacket() * af.getFrameSize()];
        while (sound.remaining() > 0) {
            AudioFrame frame = audioSource.getFrame(buf);
            if (frame == null)
                break;
            Buffer data = frame.getData();
            sound.write(data.read(Math.min(data.remaining(), sound.remaining())));
        }

        ao.flush();
        ao.resume();
        ao.write(fork);
        ao.drain();
        ao.pause();
    }

    public void next() {
        executor.submit(new Runnable() {
            public void run() {
                try {
                    nextInt();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public RationalLarge getPos() {
        return pts;
    }
}