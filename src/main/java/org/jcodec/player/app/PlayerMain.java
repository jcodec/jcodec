package org.jcodec.player.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.Player;
import org.jcodec.player.Stepper;
import org.jcodec.player.filters.JCodecAudioSource;
import org.jcodec.player.filters.JCodecVideoSource;
import org.jcodec.player.filters.JSoundAudioOut;
import org.jcodec.player.filters.http.HttpMedia;
import org.jcodec.player.filters.http.HttpPacketSource;
import org.jcodec.player.ui.SwingVO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PlayerMain {
    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Player");

        Timer cacheEvent = new Timer(true);

        SwingVO vo = new SwingVO();
        frame.getContentPane().add(vo, BorderLayout.CENTER);

        // Finish setting up the frame, and show it.
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        vo.setVisible(true);

        File cacheWhere = new File(System.getProperty("user.home"), "Library/JCodec");
        FileUtils.forceMkdir(cacheWhere);
        URL url = new URL(args[0]);
        HttpMedia http = new HttpMedia(url, cacheWhere);

        final HttpPacketSource videoTrack = http.getVideoTrack();
        JCodecVideoSource video = new JCodecVideoSource(videoTrack);
        JCodecAudioSource audio = new JCodecAudioSource(http.getAudioTracks().get(0));
        final Player player = new Player(video, audio, vo, new JSoundAudioOut());
        final Stepper stepper = new Stepper(video, audio, vo, new JSoundAudioOut());

        frame.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (!player.pause()) {
                        stepper.seek(player.getPos());
                    } else {
                        player.seek(stepper.getPos());
                        player.play();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    RationalLarge pos = player.getPos();
                    player.seek(new RationalLarge(pos.getNum() - pos.getDen() * 100, pos.getDen()));
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    RationalLarge pos = player.getPos();
                    player.seek(new RationalLarge(pos.getNum() + pos.getDen() * 100, pos.getDen()));
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    // player.prevFrame();
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    if (!player.pause()) {
                        stepper.seek(player.getPos());
                        return;
                    }
                    stepper.next();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    player.destroy();
                    System.exit(-1);
                }
            }
        });

        frame.pack();
        frame.setVisible(true);
        frame.setSize(new Dimension(768, 596));

        cacheEvent.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                List<int[]> cached = videoTrack.getCached(100);
                for (int[] is : cached) {
                    System.out.println(is[0] + ":" + is[1]);
                }

            }
        }, 5000, 5000);

        player.play();
    }
}
