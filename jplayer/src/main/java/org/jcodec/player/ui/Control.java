package org.jcodec.player.ui;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface Control {

    void display(Image img);

    boolean handleEvent(Event event);
}