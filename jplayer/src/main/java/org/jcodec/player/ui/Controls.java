package org.jcodec.player.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.player.Player;
import org.jcodec.player.ui.DraggableButton.DragListener;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Controls {
    public static int WIDTH = 400;
    public static int STRIDE = WIDTH * 2;
    public static int HEIGHT = 92;
    public static int BOTTOM = 200;

    public static int DELAY = 1000;

    private int alpha;
    private State state;
    private Timer timer;
    private TimerTask task;
    private Picture offscreen;

    private int posX;
    private int posY;

    private int bufferLevel;

    private List<Control> controls = new ArrayList<Control>();
    private DraggableButton playhead;
    private Player player;

    enum State {
        HIDDEN, ACTIVE, HIDING, SHOWING
    }

    public Controls(URL resourcePath, final Player player) throws Exception {
        this.player = player;
        controls.add(new Button(ImageIO.read(new URL(resourcePath, "prev.png")), ImageIO.read(new URL(resourcePath,
                "prev_p.png")), 60, 12, new Runnable() {
            public void run() {
//                player.prevFrame();
            }
        }));
        controls.add(new Button(ImageIO.read(new URL(resourcePath, "play.png")), ImageIO.read(new URL(resourcePath,
                "play_p.png")), 142, 6, new Runnable() {
            public void run() {
//                player.play();
            }
        }));
        controls.add(new Button(ImageIO.read(new URL(resourcePath, "pause.png")), ImageIO.read(new URL(resourcePath,
                "pause_p.png")), 210, 6, new Runnable() {
            public void run() {
//                player.pause();
            }
        }));
        controls.add(new Button(ImageIO.read(new URL(resourcePath, "next.png")), ImageIO.read(new URL(resourcePath,
                "next_p.png")), 308, 12, new Runnable() {
            public void run() {
//                player.nextFrame();
            }
        }));
        playhead = new DraggableButton(ImageIO.read(new URL(resourcePath, "playhead.png")),
                ImageIO.read(new URL(resourcePath, "playhead_p.png")), 0, 62, new DragListener() {
                    public void drag(DraggableButton ths) {
//                        player.seekRel(100 * ths.getX() / WIDTH);
                    }
                }, WIDTH - 20);
        controls.add(playhead);
        state = State.HIDDEN;
        timer = new Timer();
        offscreen = Picture.create(WIDTH, HEIGHT, ColorSpace.YUV420);
        bufferLevel = 50;
    }

    public void draw(Picture f) {
        posX = (f.getWidth() - WIDTH) >> 1;
        posY = f.getHeight() - BOTTOM - HEIGHT;
        if (state == State.SHOWING) {
            if (alpha < 5)
                alpha += 1;
            else {
                state = State.ACTIVE;
                scheduleClose();
            }
        } else if (state == State.HIDING) {
            if (alpha > 0)
                alpha -= 1;
            else
                state = State.HIDDEN;
        }

        if (state != State.HIDDEN) {
            int val;
            if (state == State.SHOWING)
                val = 64 + alpha * 10;
            else if (state == State.HIDING) {
                val = alpha * 10;
            } else
                val = 114;
            
//            playhead.x = (int)((player.getPositionRel() * WIDTH) / 100);

//            YUY2.bar(offscreen, STRIDE, 0, 0, WIDTH, HEIGHT, 0, 128, 128);
//            for (Control control : controls) {
//                control.display(new Image(offscreen, STRIDE, WIDTH, HEIGHT));
//            }
//            YUY2.bar(offscreen, STRIDE, 10, HEIGHT - 30, (WIDTH - 20), 15, 128, 128, 128);
//            YUY2.bar(offscreen, STRIDE, 10, HEIGHT - 30, ((WIDTH - 20) * bufferLevel) / 100, 15, 222, 128, 128);
//            YUY2.blend(picture, posX, posY, val, offscreen, WIDTH, HEIGHT);
        }
    }

//    public void handleEvent(NSEvent nsEvent) {
//        if (nsEvent.type() == 1 || nsEvent.type() == 2 || nsEvent.type() == 5 || nsEvent.type() == 6) {
//            NSRect frame = nsEvent.window().frame();
//            int absX = (int) ((1920 * nsEvent.locationInWindow().x) / frame.width);
//            int absY = (int) (1080 - (1080 * nsEvent.locationInWindow().y) / frame.height);
//
//            int relX = absX - posX;
//            int relY = absY - posY;
//            boolean notDispatch = (nsEvent.type() == 1 || nsEvent.type() == 5)
//                    && (relX < 0 || relY < 0 || relX >= WIDTH || relY >= HEIGHT);
//            if (!notDispatch) {
//                Event evt = new Event((int) nsEvent.type(), (int) nsEvent.buttonNumber(),
//                        (int) nsEvent.locationInWindow().x, (int) (frame.height - nsEvent.locationInWindow().y), relX,
//                        relY);
//                for (Control control : controls) {
//                    if (control.handleEvent(evt))
//                        break;
//                }
//            }
//        }
//        if (nsEvent.type() == 5 || nsEvent.type() == 1 || nsEvent.type() == 2 || nsEvent.type() == 6) {
//            if (state == State.HIDDEN) {
//                state = State.SHOWING;
//            } else if (state == State.ACTIVE) {
//                scheduleClose();
//            }
//        }
//    }

    private void scheduleClose() {
        if (task != null)
            task.cancel();
        task = new TimerTask() {
            public void run() {
                state = State.HIDING;
            }
        };
        timer.schedule(task, 1000);
    }

    public int squereIn(int x) {
        return x * x;
    }

    public void setBufferLevel(int bufferLevel) {
        this.bufferLevel = bufferLevel;
    }

    public void draw(Frame frame) {
        // TODO Auto-generated method stub
        
    }
}
