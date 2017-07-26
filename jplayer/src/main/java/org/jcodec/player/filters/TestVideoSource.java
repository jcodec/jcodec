package org.jcodec.player.filters;

import static org.jcodec.common.model.ColorSpace.YUV420;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class TestVideoSource implements VideoSource {
    private static final int DURATION_TV = 2502500;
    private static final int HEIGHT = 1080;
    private static final int WIDTH = 1920;
    private long pts = 0;
    private BufferedImage img;

    public TestVideoSource() {
        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
    }

    public Frame decode(byte[][] buf) throws IOException {
        if (pts > DURATION_TV)
            return null;

        Graphics2D g2d = img.createGraphics();
        g2d.setPaint(Color.WHITE);
        g2d.setFont(new Font("Serif", Font.BOLD, 50));
        FontMetrics fm = g2d.getFontMetrics();
        String s = String.valueOf(pts);
        int stringWidth = fm.stringWidth(s);
        int x = (img.getWidth() - stringWidth) / 2;
        int y = (img.getHeight() - fm.getHeight()) / 2;
        g2d.drawString(s, x, y);
        g2d.dispose();

        pts += 1001;

        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < data.length; i++) {
            buf[0][i] = (byte)((data[i] & 0xff) - 128);
            data[i] = 0;
        }

        return new Frame(Picture8Bit.createPicture8Bit(WIDTH, HEIGHT, buf, YUV420), new RationalLarge(pts, 24000),
                new RationalLarge(1001, 24000), new Rational(1, 1), 0, null, null);
    }

    public void seek(RationalLarge second) {
        pts = second.multiplyS(24000);
        if (pts > DURATION_TV)
            pts = DURATION_TV - 1001;
        if (pts < 0)
            pts = 0;
    }

    public MediaInfo.VideoInfo getMediaInfo() {
        return new MediaInfo.VideoInfo("test", 24000, DURATION_TV, DURATION_TV / 1001, "", null, new Rational(1, 1),
                new Size(WIDTH, HEIGHT));
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean drySeek(RationalLarge second) throws IOException {
        return true;
    }

    @Override
    public void gotoFrame(int frameNo) {
        pts = frameNo * 1001;
    }
}