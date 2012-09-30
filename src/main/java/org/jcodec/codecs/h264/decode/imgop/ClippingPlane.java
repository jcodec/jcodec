package org.jcodec.codecs.h264.decode.imgop;

import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A pixel plane that is capable to clip drawings
 * 
 * 
 * @author Jay Codec
 * 
 */
public class ClippingPlane {
    private int offX;
    private int offY;
    private int width;
    private int height;
    private int[] buf;

    public ClippingPlane(Rect crop) {
        this.offX = crop.getX();
        this.offY = crop.getY();
        this.width = crop.getWidth();
        this.height = crop.getHeight();
        this.buf = new int[width * height];
    }

    public void putBlock(int x, int y, int[] pix, int w, int h) {
        if ((x + w < offX) || (y + h < offY) || (x > offX + width) || (y > offY + height))
            return;
        int pL = offX - x;
        int pT = offY - y;
        int pR = x + w - (offX + width);
        int pB = y + h - (offY + height);

        pL = (pL >= 0 ? pL : 0);
        pT = (pT >= 0 ? pT : 0);
        pR = (pR >= 0 ? pR : 0);
        pB = (pB >= 0 ? pB : 0);

        int sO = (y - offY) * width + (x - offX);
        int dO = (pT << 4) + pL;
        for (int j = 0; j < h - pB - pT; j++) {
            for (int i = 0; i < w - pR - pL; i++) {
                buf[sO + i] = pix[dO + i];
            }
            sO += width;
            dO += w;
        }
    }

    public int[] getBuf() {
        return buf;
    }
}