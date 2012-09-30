package org.jcodec.player.app;

import static java.lang.Math.round;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JApplet;

import netscape.javascript.JSObject;

import org.apache.commons.io.FileUtils;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.tools.Debug;
import org.jcodec.player.Player;
import org.jcodec.player.Player.Listener;
import org.jcodec.player.Player.Status;
import org.jcodec.player.filters.JCodecAudioSource;
import org.jcodec.player.filters.JCodecVideoSource;
import org.jcodec.player.filters.JSoundAudioOut;
import org.jcodec.player.filters.MediaInfo.VideoInfo;
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

    private JCodecAudioSource audio;

    private SwingVO vo;

    public JSObject onTimeChanged;
    public JSObject onStatusChanged;
    public JSObject onCacheChanged;

    private Timer cacheEvent = new Timer(true);

    // private Stepper stepper;

    public void open(final String src) {
        Debug.println("Opening source: " + src);
        destroy();
        try {
            HttpMedia http = AccessController.doPrivileged(new PrivilegedAction<HttpMedia>() {
                public HttpMedia run() {
                    try {
                        File cacheWhere = determineCacheLocation();
                        FileUtils.forceMkdir(cacheWhere);
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
            audio = new JCodecAudioSource(http.getAudioTracks().get(0));
            player = new Player(video, audio, vo, new JSoundAudioOut());

            player.addListener(new Listener() {
                public void timeChanged(RationalLarge pts, int frameNo, TapeTimecode tt) {
                    if (onTimeChanged == null)
                        return;
                    VideoInfo videoInfo;
                    videoInfo = player.getVideoSource().getMediaInfo();
                    onTimeChanged.call("call", new Object[] {
                            null,
                            (double) pts.getNum() / pts.getDen(),
                            (double) videoInfo.getDuration() / videoInfo.getTimescale(),
                            frameNo,
                            tt == null ? null
                                    : new int[] { tt.getHour(), tt.getMinute(), tt.getSecond(), tt.getFrame() } });
                }

                public void statusChanged(Status status) {
                    if (onStatusChanged != null)
                        onStatusChanged.call("call", new Object[] { null, status.toString() });
                }
            });
            cacheEvent.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    if (onCacheChanged == null)
                        return;
                    List<int[]> cached = videoTrack.getCached(100);
                    onCacheChanged.call("call", new Object[] { null, cached.toArray(new int[0][]),
                            videoTrack.getMediaInfo().getNFrames() });
                }
            }, 5000, 5000);
            // stepper = new Stepper(video, audio, new SwingVO(this), new
            // JSoundAudioOut());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open source", e);
        }
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

        // player.seek(stepper.getPos());
        player.play();
    }

    public void pause() {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        if (!player.pause()) {
            // stepper.seek(player.getPos());
        }
    }

    public void togglePause() {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        if (!player.pause()) {
            // stepper.seek(player.getPos());
        } else {
            // player.seek(stepper.getPos());
            player.play();
        }
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

    public void step() {
        if (player == null)
            throw new IllegalArgumentException("player is not initialized");

        // try {
        // player.step();
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
}