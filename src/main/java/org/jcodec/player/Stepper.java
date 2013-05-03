package org.jcodec.player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;
import org.jcodec.player.Player.Listener;
import org.jcodec.player.filters.AudioOut;
import org.jcodec.player.filters.MediaInfo.AudioInfo;
import org.jcodec.player.filters.MediaInfo.VideoInfo;
import org.jcodec.player.filters.VideoOutput;
import org.jcodec.player.filters.VideoSource;
import org.jcodec.player.filters.audio.AudioSource;
import org.jcodec.scale.ColorUtil;

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

    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoOutput vo;
    private AudioOut ao;
    private VideoInfo mi;
    private AudioInfo ai;
    private Picture dst;

    private RationalLarge pts;
    // private int frameNo;
    private int[][] target;
    private List<Player.Listener> listeners = new ArrayList<Player.Listener>();
    private Timer timer = new Timer();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AudioFormat af;

    public Stepper(VideoSource videoSource, AudioSource audioSource, VideoOutput vo, AudioOut ao) throws IOException {
        this.videoSource = videoSource;
        this.vo = vo;
        this.ao = ao;
        this.audioSource = audioSource;
        this.mi = videoSource.getMediaInfo();

        ai = this.audioSource.getAudioInfo();
        af = ai.getFormat();
        ao.open(af, af.getFrameSize() * (int) af.getFrameRate());
    }

    private int[][] createTarget() {
        Size dim = mi.getDim();
        int sz = 2 * dim.getWidth() * dim.getHeight();
        return new int[][] { new int[sz], new int[sz], new int[sz] };
    }

    private void nextInt() throws IOException {
        // frameNo++;
        // curInt();
        // }

        // private void curInt() throws IOException {
        if (target == null)
            target = createTarget();
        // System.out.println(frameNo);
        // videoSource.gotoFrame(frameNo);
        Frame decode = videoSource.decode(target);
        RationalLarge pts = decode.getPts();
        if (audioSource.drySeek(pts)) {
            audioSource.seek(pts);
            playSound(150);
        }
        show(decode);
    }

    // private void prevInt() throws IOException {
    // frameNo--;
    // curInt();
    // }

    private void show(Frame frame) {
        pts = frame.getPts();
        fireTimeEvent(frame);
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

    private void fireTimeEvent(final Frame frame) {
        timer.schedule(new TimerTask() {
            public void run() {
                for (final Listener listener : listeners) {
                    try {
                        listener.timeChanged(frame.getPts(), frame.getFrameNo(), frame.getTapeTimecode());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }, 0);
    }

    private void playSound(int ms) throws IOException {
        ByteBuffer sound = ByteBuffer.allocate(((int) (ms * af.getFrameRate() / 1000) * af.getFrameSize()));
        ByteBuffer buf = ByteBuffer.allocate(ai.getFramesPerPacket() * af.getFrameSize());
        while (sound.remaining() > 0) {
            AudioFrame frame = audioSource.getFrame(buf);
            if (frame == null)
                break;
            ByteBuffer data = frame.getData();
            NIOUtils.write(sound, NIOUtils.read(data, Math.min(data.remaining(), sound.remaining())));
        }
        sound.flip();

        ao.flush();
        ao.resume();
        ao.write(sound);
        ao.drain();
        ao.pause();
    }

    public void next() {
        executor.submit(new Runnable() {
            public void run() {
                try {
                    nextInt();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    public void prev() {
        // executor.submit(new Runnable() {
        // public void run() {
        // try {
        // prevInt();
        // } catch (Throwable t) {
        // t.printStackTrace();
        // }
        // }
        // });
    }

    public RationalLarge getPos() {
        return pts;
    }

    public void gotoFrame(int frame) {
        videoSource.gotoFrame(frame);
        // frameNo = frame;
    }

    public void setListeners(List<Player.Listener> listeners) {
        this.listeners = listeners;
    }
}