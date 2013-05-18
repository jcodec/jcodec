package org.jcodec.player.app;

import static java.lang.Math.round;
import static org.jcodec.common.IOUtils.forceMkdir;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JApplet;

import netscape.javascript.JSObject;

import org.jcodec.common.IOUtils;
import org.jcodec.common.StringUtils;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.tools.Debug;
import org.jcodec.player.Player;
import org.jcodec.player.Player.Listener;
import org.jcodec.player.Player.Status;
import org.jcodec.player.Stepper;
import org.jcodec.player.filters.JCodecVideoSource;
import org.jcodec.player.filters.JSoundAudioOut;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.MediaInfo.AudioInfo;
import org.jcodec.player.filters.MediaInfo.VideoInfo;
import org.jcodec.player.filters.audio.AudioMixer;
import org.jcodec.player.filters.audio.AudioMixer.Pin;
import org.jcodec.player.filters.audio.AudioSource;
import org.jcodec.player.filters.audio.JCodecAudioSource;
import org.jcodec.player.filters.http.HttpMedia;
import org.jcodec.player.filters.http.HttpPacketSource;
import org.jcodec.player.ui.SwingVO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Media player applet for JCodec streaming
 * 
 * @author The JCodec project
 * 
 */
public class PlayerApplet extends JApplet {

    /**
     * 
     */
    private static final long serialVersionUID = -6134123851724553559L;

    private Player player;

    private JCodecVideoSource video;

    private SwingVO vo;

    public JSObject onStatus1;
    public JSObject onStatus2;
    public JSObject onStateChanged;

    private Timer timer = new Timer(true);

    private AudioMixer audio;

    private TimerTask status2Task;

    private Stepper stepper;

    public class Status1 {
        public double time;
        public int frame;
        public int[] tapeTimecode;

        public Status1(double time, int frame, int[] tapeTimecode) {
            this.time = time;
            this.frame = frame;
            this.tapeTimecode = tapeTimecode;
        }
    };

    public class Status2 {

        public double duration;
        public int nFrames;
        public int[][] cache;
        public int[][] audio;

        public Status2(double duration, int nFrames, int[][] cache, int[][] audio) {
            this.duration = duration;
            this.nFrames = nFrames;
            this.cache = cache;
            this.audio = audio;
        }
    };

    public void open(final String src) {
        Debug.println("Opening source: " + src);
        try {
            if (player != null) {
                Debug.println("Destroying old player.");
                player.destroy();
                video.close();
                audio.close();
                status2Task.cancel();
                status2Task = null;
            }

            HttpMedia http = AccessController.doPrivileged(new PrivilegedAction<HttpMedia>() {
                public HttpMedia run() {
                    try {
                        File cacheWhere = determineCacheLocation();
                        forceMkdir(cacheWhere);
                        return new HttpMedia(new URL(src), cacheWhere);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Could not open HTTP source: '" + src + "'");
                    }
                }
            });
            Debug.println("Initialized packet source");

            Debug.println("Creating player");
            final HttpPacketSource videoTrack = http.getVideoTrack();
            video = new JCodecVideoSource(videoTrack);
            audio = audio(http);
            player = new Player(video, audio, vo, new JSoundAudioOut());

            player.addListener(new Listener() {
                public void timeChanged(RationalLarge pts, int frameNo, TapeTimecode tt) {
                    if (onStatus1 == null)
                        return;
                    onStatus1.call(
                            "call",
                            new Object[] {
                                    null,
                                    new Status1((double) pts.getNum() / pts.getDen(), frameNo, tt != null ? new int[] {
                                            tt.getHour(), tt.getMinute(), tt.getSecond(), tt.getFrame(),
                                            tt.isDropFrame() ? 1 : 0 } : null) });
                }

                public void statusChanged(Status status) {
                    if (onStateChanged != null)
                        onStateChanged.call("call", new Object[] { null, status.toString() });
                }
            });
            status2Task = new TimerTask() {
                public void run() {
                    if (onStatus2 == null)
                        return;

                    try {
                        VideoInfo videoInfo = player.getVideoSource().getMediaInfo();
                        onStatus2.call("call", new Object[] {
                                null,
                                new Status2((double) videoInfo.getDuration() / videoInfo.getTimescale(),
                                        (int) videoInfo.getNFrames(), videoTrack.getCached(), enabledChannels()) });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer.scheduleAtFixedRate(status2Task, 0, 1000);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open source", e);
        }
    }

    private AudioMixer audio(HttpMedia http) throws IOException {
        List<HttpPacketSource> audioTracks = http.getAudioTracks();
        AudioSource[] audios = new AudioSource[audioTracks.size()];
        for (int i = 0; i < audioTracks.size(); i++) {
            audios[i] = new JCodecAudioSource(audioTracks.get(i));
        }
        return new AudioMixer(2, audios);
    }

    public class AudioTrack {
        public AudioTrack(String name, String[] channels) {
            this.name = name;
            this.channels = channels;
        }

        public String name;
        public String[] channels;
    }

    public AudioTrack[] getAudioConfig() {
        try {
            Pin[] pins = audio.getPins();
            AudioTrack[] ats = new AudioTrack[pins.length];
            for (int i = 0; i < pins.length; i++) {
                AudioInfo ai = pins[i].getSource().getAudioInfo();
                ats[i] = new AudioTrack(ai.getName(), channels(ai.getLabels()));
            }
            return ats;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new AudioTrack[0];
    }

    private String[] channels(ChannelLabel[] labels) {
        String[] strs = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            strs[i] = labels[i] != null ? StringUtils.capitaliseAllWords(labels[i].toString().toLowerCase().replace("_", " "))
                    : "N/A";
        }
        return strs;
    }

    public void toggleChannel(int trackId, int channelId) {
        audio.getPins()[trackId].toggle(channelId);
    }

    public void muteChannel(int trackId, int channelId) {
        audio.getPins()[trackId].mute(channelId);
    }

    public void unmuteChannel(int trackId, int channelId) {
        audio.getPins()[trackId].unmute(channelId);
    }

    private int[][] enabledChannels() {
        Pin[] pins = audio.getPins();
        int[][] result = new int[pins.length][];
        for (int i = 0; i < pins.length; i++) {
            result[i] = pins[i].getSoloChannels();
        }
        return result;
    }

    @Override
    public void init() {
        this.setBackground(Color.BLACK);

        vo = new SwingVO();
        add(vo, BorderLayout.CENTER);
        validate();

        String src = this.getParameter("src");
        if (src != null) {
            open(src);
            if ("true".equalsIgnoreCase(this.getParameter("play"))) {
                player.play();
            }
        }
    }

    @Override
    public void stop() {
        Debug.println("Stopping player");
        if (player == null)
            return;
        player.destroy();
        try {
            video.close();
            audio.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not destroy player", e);
        }
    }

    public void play() {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");
        Debug.println("Starting playback.");

        if (stepper != null) {
            player.seek(stepper.getPos());
            stepper = null;
        }
        player.play();
    }

    public void pause() {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        player.pause();
    }

    public void togglePause() {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        if (player.getStatus() == Player.Status.PAUSED) {
            if (stepper != null) {
                player.seek(stepper.getPos());
                stepper = null;
            }
            player.play();
        } else
            player.pause();
    }

    public void seekRel(int sec) {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        RationalLarge pos = player.getPos();
        try {
            player.seek(new RationalLarge(pos.getNum() + pos.getDen() * sec, pos.getDen()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not relative seek", e);
        }
    }

    public void seek(double sec) {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        try {
            player.seek(new RationalLarge(round(sec * 1000000), 1000000));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not seek", e);
        }
    }

    public void stepForward() {
        if (player.getStatus() != Player.Status.PAUSED) {
            player.pause();
            return;
        }
        try {
            if (stepper == null) {
                stepper = new Stepper(video, audio, vo, new JSoundAudioOut());
                stepper.setListeners(player.getListeners());
                stepper.gotoFrame(player.getFrameNo());
            }
            stepper.next();
        } catch (Exception e) {
            throw new RuntimeException("Could not step", e);
        }
    }

    public void stepBackward() {
        // if (player.getStatus() != Player.Status.PAUSED) {
        // player.pause();
        // return;
        // }
        // try {
        // if (stepper == null) {
        // stepper = new Stepper(video, audio, vo, new JSoundAudioOut());
        // stepper.setListeners(player.getListeners());
        // stepper.gotoFrame(player.getFrameNo());
        // }
        // stepper.prev();
        // } catch (Exception e) {
        // throw new RuntimeException("Could not step", e);
        // }
    }

    private File determineCacheLocation() {
        String os = System.getProperty("os.name");
        String home = System.getProperty("user.home");

        if (os.startsWith("Mac")) {
            return new File(home, "Library/JCodec");
        } else if (os.startsWith("Windows")) {
            return new File(home, "Application Data/JCodec");
        } else {
            return new File(home, ".jcodec");
        }
    }

    public double getTime() {
        RationalLarge t = player.getPos();
        return ((double) t.getNum()) / t.getDen();
    }

    public MediaInfo[] getMediaInfo() throws IOException {
        ArrayList<MediaInfo> info = new ArrayList<MediaInfo>();
        info.add(sourceInfo(video.getMediaInfo()));
        Pin[] pins = audio.getPins();
        for (Pin pin : pins) {
            info.add(sourceInfo(pin.getSource().getAudioInfo()));
        }
        return info.toArray(new MediaInfo[0]);
    }

    private MediaInfo sourceInfo(MediaInfo mediaInfo) {
        MediaInfo result = mediaInfo;
        while (result.getTranscodedFrom() != null)
            result = result.getTranscodedFrom();
        return result;
    }
}