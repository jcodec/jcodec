package org.jcodec.player.ui;

import java.awt.image.BufferedImage;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DraggableButton extends Button {

    private int max;
    private int min;
    private DragListener lsnr;

    public DraggableButton(BufferedImage normal, BufferedImage pressed, int x, int y, DragListener lsnr, int max) {
        super(normal, pressed, x, y, null);
        this.max = max;
        this.min = x;
        this.lsnr = lsnr;
    }

    public boolean handleEvent(Event evt) {
        if (isPressed()) {
            x = evt.getX() - normal.getWidth() / 2;
            if (x > max)
                x = max;
            else if (x < min)
                x = min;
            lsnr.drag(this);
        }
        return super.handleEvent(evt);
    }

    public int getX() {
        return x;
    }

    public static interface DragListener {
        void drag(DraggableButton ths);
    }
}
