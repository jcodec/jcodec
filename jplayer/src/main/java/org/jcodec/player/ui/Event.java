package org.jcodec.player.ui;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Event {
    private int button;
    private int absX;
    private int absY;
    private int x;
    private int y;
    private int type;

    public Event(int type, int button, int absX, int absY, int x, int y) {
        this.type = type;
        this.button = button;
        this.absX = absX;
        this.absY = absY;
        this.x = x;
        this.y = y;
    }
    

    public int getType() {
        return type;
    }

    public int getButton() {
        return button;
    }

    public int getAbsX() {
        return absX;
    }

    public int getAbsY() {
        return absY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}