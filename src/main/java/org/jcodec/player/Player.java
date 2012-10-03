package org.jcodec.player;

import static org.jcodec.player.util.ThreadUtil.joinForSure;
import static org.jcodec.player.util.ThreadUtil.sleepNoShit;
import static org.jcodec.player.util.ThreadUtil.surePut;
import static org.jcodec.player.util.ThreadUtil.sureTake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.tools.Debug;
import org.jcodec.player.filters.AudioOut;
import org.jcodec.player.filters.AudioSource;
import org.jcodec.player.filters.ChannelSelector;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.MediaInfo.AudioInfo;
import org.jcodec.player.filters.Resampler24To16;
import org.jcodec.player.filters.ToStereo;
import org.jcodec.player.filters.VideoOutput;
import org.jcodec.player.filters.VideoSource;
import org.jcodec.scale.ColorUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Media player engine
 * 
 * @author The JCodec project
 * 
 */
public class Player {
    public enum Status {
        STOPPED, PAUSED, BUFFERING, PLAYING
    }

    private static final int VIDEO_QUEUE_SIZE = 50;
    private static final int AUDIO_QUEUE_SIZE = 50;
    public static final int PACKETS_IN_BUFFER = 8;
    public static int TIMESCALE = 96000;

    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoOutput vo;
    private AudioOut ao;

    AtomicBoolean pause = new AtomicBoolean();
    private long clock;
    private long lastAudio;

    private List<Frame> video = Collections.synchronizedList(new ArrayList<Frame>());
    private BlockingQueue<int[][]> videoDrain = new LinkedBlockingQueue<int[][]>();

    private volatile boolean stop;

    private BlockingQueue<Buffer> audio = new LinkedBlockingQueue<Buffer>();
    private BlockingQueue<byte[]> audioDrain = new LinkedBlockingQueue<byte[]>();

    private AudioFormat af;
    private Picture dst;
    private Object seekLock = new Object();
    private Object pausedEvent = new Object();
    private MediaInfo.VideoInfo mi;
    private int audioPacketSize;

    private List<Listener> listeners = new ArrayList<Listener>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean resume;
    private volatile boolean decodingLocked;

    private Thread resumeThread;
    private Thread videoPlaybackThread;
    private Thread audioDecodeThread;
    private Thread audioPlaybackThread;
    private Thread videoDecodeThread;

    public Player(VideoSource videoSource, AudioSource audioSource, VideoOutput vo, AudioOut ao) throws IOException {
        this.videoSource = videoSource;
        this.audioSource = audioSource;
        this.vo = vo;
        this.ao = ao;

        initPlayer();
    }

    private void initPlayer() throws IOException {
        Debug.println("Initializing player");

        pause.set(true);
        clock = 0;

        videoDrain.clear();
        audioDrain.clear();
        video.clear();
        audio.clear();

        prepareAudioSource();
        AudioInfo ai = audioSource.getAudioInfo();
        af = ai.getFormat();
        audioPacketSize = ai.getFramesPerPacket();
        ao.open(af, audioPacketSize * PACKETS_IN_BUFFER);

        mi = videoSource.getMediaInfo();

        startAudioDecode();

        lastAudio = ao.playedMs();
        startAudioPlayback();

        startVideoDecode();

        for (int i = 0; i < VIDEO_QUEUE_SIZE; i++) {
            surePut(videoDrain, createTarget());
        }

        for (int i = 0; i < AUDIO_QUEUE_SIZE; i++) {
            surePut(audioDrain, new byte[af.getFrameSize() * (audioPacketSize + 10)]);
        }

        startVideoPlayback();

        startResumeThread();
    }

    /**
     * Resumes player playback as soon as possible
     */
    public void play() {
        resume = true;
        notifyStatus();
    }

    /**
     * Pauses playback
     * 
     * Waits until player actually stops
     * 
     * @return Wheather playback was already paused
     */
    public boolean pause() {
        resume = false;
        return pauseWait();
    }

    private void startResumeThread() {
        resumeThread = new Thread() {
            public void run() {
                while (!stop) {
                    if (resume && pause.get()) {
                        if (audio.size() >= AUDIO_QUEUE_SIZE / 2 && video.size() >= VIDEO_QUEUE_SIZE / 2) {
                            pause.set(false);
                            ao.resume();
                            notifyStatus();
                        }
                    }
                    sleepNoShit(500000);
                }
            }
        };
        resumeThread.setDaemon(true);
        resumeThread.start();
    }

    private void startVideoPlayback() {
        videoPlaybackThread = new Thread() {
            public void run() {
                Debug.println("Starting video playback");
                try {
                    playVideo();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Debug.println("Playing video done");
            }
        };
        videoPlaybackThread.start();
    }

    Frame[] EMPTY = new Frame[0];

    private void playVideo() throws IOException {
        while (!stop) {
            if (!pause.get()) {
                long newAudio = ao.playedMs();
                clock += newAudio - lastAudio;
                lastAudio = newAudio;

                long pts = (clock * 96) / 1000;

                Frame selected = selectFrame(pts);
                if (selected == null) {
                    if (video.size() > 0)
                        sleepNoShit(2000000);
                    else
                        pauseNoWait();
                } else {
                    show(selected);
                    surePut(videoDrain, selected.getPic().getData());
                }
            } else {
                synchronized (pausedEvent) {
                    pausedEvent.notifyAll();
                }
                sleepNoShit(200000);
            }
        }
    }

    private Frame selectFrame(long pts) {
        List<Frame> remove = new ArrayList<Frame>();
        Frame closest = null;
        long closestPts = 0;
        for (Frame frame : video.toArray(EMPTY)) {
            long framePts = (frame.getPts().getNum() * TIMESCALE) / frame.getPts().getDen();
            if (framePts > pts)
                continue;

            if (closest == null || framePts > closestPts) {
                if (closest != null)
                    remove.add(closest);
                closest = frame;
                closestPts = framePts;
            }
        }
        video.removeAll(remove);
        for (Frame frame : remove) {
            surePut(videoDrain, frame.getPic().getData());
        }
        if (closest != null)
            video.remove(closest);

        return closest;
    }

    private int[][] createTarget() {
        Size dim = mi.getDim();
        int sz = 2 * dim.getWidth() * dim.getHeight();
        return new int[][] { new int[sz], new int[sz], new int[sz] };
    }

    private void startVideoDecode() {
        videoDecodeThread = new Thread() {
            public void run() {
                Debug.println("Starting video decode");
                try {
                    decodeVideo();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Debug.println("Decoding video done");
            }
        };
        videoDecodeThread.start();
    }

    private void startAudioDecode() {
        audioDecodeThread = new Thread() {
            public void run() {
                Debug.println("Starting audio decode");
                try {
                    decodeAudio();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Debug.println("Decoding audio done");
            }
        };
        audioDecodeThread.start();
    }

    private void decodeAudio() throws IOException {
        long predPts = Long.MIN_VALUE;
        while (!stop) {
            if (decodingLocked) {
                sleepNoShit(500000);
                continue;
            }

            byte[] buf = sureTake(audioDrain);
            AudioFrame frame = audioSource.getFrame(buf);

            if (frame != null) {
                long pts = (frame.getPts() * TIMESCALE) / frame.getTimescale();

                if (Math.abs(predPts - pts) > TIMESCALE / 100) {
                    while (pause.get() != true)
                        sleepNoShit(500000);
                    clock = (1000000 * frame.getPts()) / frame.getTimescale();
                    if (!seekVideo(pts, TIMESCALE))
                        seekVideo(pts + TIMESCALE / 100, TIMESCALE);
                }

                predPts = (frame.getPts() * TIMESCALE) / frame.getTimescale() + (frame.getDuration() * TIMESCALE)
                        / frame.getTimescale();

                surePut(audio, frame.getData());
            } else {
                surePut(audioDrain, buf);
                sleepNoShit(500000);
            }
        }
    }

    private void decodeVideo() throws IOException {
        while (!stop) {
            if (decodingLocked) {
                sleepNoShit(500000);
                continue;
            }

            decodeJustOneFrame();
        }
    }

    private void decodeJustOneFrame() throws IOException {
        int[][] buf = sureTake(videoDrain);
        Frame frame = videoSource.decode(buf);
        if (frame != null) {
            video.add(frame);
        } else {
            surePut(videoDrain, buf);
            sleepNoShit(500000);
        }
    }

    private void startAudioPlayback() {
        audioPlaybackThread = new Thread() {
            public void run() {
                sleepNoShit(10000000);
                playAudio();
            }
        };

        audioPlaybackThread.start();
    }

    private void playAudio() {
        Debug.println("Starting audio playback");
        Buffer pkt = null;
        while (!stop) {
            if (!pause.get()) {
                if (pkt == null) {
                    pkt = audio.poll();
                    if (pkt == null) {
                        Debug.println("Audio queue empty");
                        pauseNoWait();
                        continue;
                    }
                }
                pkt.pos += ao.write(pkt.buffer, pkt.pos, pkt.remaining());
                if (pkt.remaining() == 0) {
                    surePut(audioDrain, pkt.buffer);
                    pkt = null;
                }
            } else {
                sleepNoShit(500000);
            }
        }
        Debug.println("Playing autio done");
    }

    private void prepareAudioSource() {
        audioSource = insertResampler(audioSource);
    }

    private void pauseNoWait() {
        try {
            if (!pause.getAndSet(true)) {
                ao.pause();
                Debug.println("On pause: " + ao.playedMs());
            }
        } finally {
            notifyStatus();
        }
    }

    public boolean pauseWait() {
        try {
            if (!pause.getAndSet(true)) {
                ao.pause();
                synchronized (pausedEvent) {
                    sureWait(pausedEvent);
                }
                return false;
            }
            return true;
        } finally {
            notifyStatus();
        }
    }

    private void show(Frame frame) {
        Picture src = frame.getPic();

        notifyTime(frame);
        
        if (src.getColor() != vo.getColorSpace()) {
            if (dst == null || dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight())
                dst = Picture.create(src.getWidth(), src.getHeight(), vo.getColorSpace());

            ColorUtil.getTransform(src.getColor(), vo.getColorSpace()).transform(src, dst);

            vo.show(dst, frame.getPixelAspect());
        } else {
            vo.show(src, frame.getPixelAspect());
        }
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

    private boolean seekVideo(long pts, int timescale) throws IOException {
        boolean seek;
        synchronized (seekLock) {

            decodingLocked = true;
            seek = videoSource.seek(pts, timescale);
            drainVideo();

            if (seek) {
                decodeJustOneFrame();
                if (video.size() > 0)
                    show(video.get(0));
            }

            decodingLocked = false;
        }
        return seek;
    }

    public void seek(final RationalLarge where) {
        executor.submit(new Runnable() {
            public void run() {
                try {
                    seekInt(where.getNum(), where.getDen());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void seekInt(long clk, long timescale) throws IOException {
        if (clk < 0)
            return;

        synchronized (seekLock) {
            boolean wasPlaying = resume;
            resume = false;
            pauseWait();
            decodingLocked = true;

            if (audioSource.seek(clk, timescale)) {
                drainAudio();
                ao.flush();
            }

            decodingLocked = false;

            resume = wasPlaying;
        }
    }

    private void drainVideo() {
        synchronized (video) {
            Frame[] copy = video.toArray(EMPTY);
            video.clear();
            for (Frame frame : copy) {
                surePut(videoDrain, frame.getPic().getData());
            }
        }
    }

    private void drainAudio() {
        List<Buffer> list = new LinkedList<Buffer>();
        audio.drainTo(list);
        for (Buffer frame : list) {
            audioDrain.add(frame.buffer);
        }
    }

    private void sureWait(Object monitor) {
        try {
            pausedEvent.wait();
        } catch (InterruptedException e) {
        }
    }

    public RationalLarge getPos() {
        return new RationalLarge((clock * 96) / 1000, TIMESCALE);
    }

    public void destroy() {
        stop = true;

        joinForSure(videoDecodeThread);
        joinForSure(audioDecodeThread);
        joinForSure(videoPlaybackThread);
        joinForSure(audioPlaybackThread);
        joinForSure(resumeThread);

        video = null;
        audio = null;
        videoDrain = null;
        audioDrain = null;

        Debug.println("Player destroyed");
    }

    private void notifyStatus() {
        final Status status = pause.get() ? (resume ? Status.BUFFERING : Status.PAUSED) : Status.PLAYING;
        executor.execute(new Runnable() {
            public void run() {
                for (Listener listener : listeners) {
                    try {
                        listener.statusChanged(status);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }

    private void notifyTime(final Frame frame) {
        executor.execute(new Runnable() {
            public void run() {
                for (Listener listener : listeners) {
                    try {
                        listener.timeChanged(frame.getPts(), frame.getFrameNo(), frame.getTapeTimecode());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }

    public static interface Listener {
        void timeChanged(RationalLarge pts, int frameNo, TapeTimecode tapeTimecode);

        void statusChanged(Status status);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public VideoSource getVideoSource() {
        return videoSource;
    }

    public AudioSource getAudioSources() {
        return audioSource;
    }
}