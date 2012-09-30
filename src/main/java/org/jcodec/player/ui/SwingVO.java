package org.jcodec.player.ui;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.player.filters.VideoOutput;
import org.jcodec.scale.AWTUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Video output that draws on swing panel
 * 
 * @author The JCodec project
 * 
 */
public class SwingVO extends JPanel implements VideoOutput {

    private BufferedImage img;
    private Rational pasp;

    public void show(Picture pic, Rational pasp) {

        if (img != null && img.getWidth() != pic.getWidth() && img.getHeight() != pic.getHeight())
            img = null;

        if (img == null)
            img = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        AWTUtil.toBufferedImage(pic, img);
        this.pasp = pasp;

        repaint();
    }

    @Override
    public ColorSpace getColorSpace() {
        return RGB;
    }

    @Override
    public void paint(Graphics g) {
        if (img == null)
            return;

        int width = pasp.getNum() * img.getWidth() / pasp.getDen();
        int height = (getWidth() * img.getHeight()) / width;

        g.drawImage(img, 0, ((getHeight() - height) / 2), getWidth(), height, this);
    }
}