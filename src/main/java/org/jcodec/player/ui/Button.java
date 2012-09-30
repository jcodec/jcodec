package org.jcodec.player.ui;

import java.awt.image.BufferedImage;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Button implements Control {

    protected Image normal;
    protected Image pressed;
    protected int x;
    protected int y;
    private Runnable handler;
    private Image icon;

    public Button(BufferedImage normal, BufferedImage pressed, int x, int y, Runnable handler) {
//        this.normal = YUY2.convert(normal);
//        this.pressed = YUY2.convert(pressed);
        this.x = x;
        this.y = y;
        this.handler = handler;
        this.icon = this.normal;
    }

    public void display(Image img) {
        img.draw(icon, x, y);
    }

    public boolean handleEvent(Event evt) {
        if (evt.getType() == 1 && evt.getX() >= x && evt.getY() >= y && evt.getX() < x + normal.getWidth()
                && evt.getY() < y + normal.getHeight()) {
            icon = pressed;
            if (handler != null)
                handler.run();
            return true;
        } else if (evt.getType() == 2) {
            icon = normal;
        }
        return false;
    }

    protected boolean isPressed() {
        return icon == pressed;
    }

}
