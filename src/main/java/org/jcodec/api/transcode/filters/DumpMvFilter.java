package org.jcodec.api.transcode.filters;

import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;

public class DumpMvFilter implements Filter {
    private boolean js;

    public DumpMvFilter(boolean js) {
        this.js = js;
    }

    @Override
    public LoanerPicture filter(Picture8Bit picture, PixelStore pixelStore) {
        Frame dec = (Frame) picture;
        if (!js)
            dumpMvTxt(dec);
        else
            dumpMvJs(dec);
        return null;
    }

    private void dumpMvTxt(Frame dec) {
        System.err.println("FRAME ================================================================");
        if (dec.getFrameType() == SliceType.I)
            return;
        int[][][][] mvs = dec.getMvs();
        for (int i = 0; i < 2; i++) {

            System.err.println((i == 0 ? "BCK" : "FWD")
                    + " ===========================================================================");
            for (int blkY = 0; blkY < mvs[i].length; ++blkY) {
                StringBuilder line0 = new StringBuilder();
                StringBuilder line1 = new StringBuilder();
                StringBuilder line2 = new StringBuilder();
                StringBuilder line3 = new StringBuilder();
                line0.append("+");
                line1.append("|");
                line2.append("|");
                line3.append("|");
                for (int blkX = 0; blkX < mvs[i][0].length; ++blkX) {
                    line0.append("------+");
                    line1.append(String.format("%6d|", mvs[i][blkY][blkX][0]));
                    line2.append(String.format("%6d|", mvs[i][blkY][blkX][1]));
                    line3.append(String.format("    %2d|", mvs[i][blkY][blkX][2]));
                }
                System.err.println(line0.toString());
                System.err.println(line1.toString());
                System.err.println(line2.toString());
                System.err.println(line3.toString());
            }
            if (dec.getFrameType() != SliceType.B)
                break;
        }
    }

    private void dumpMvJs(Frame dec) {
        System.err.println("{");
        if (dec.getFrameType() == SliceType.I)
            return;
        int[][][][] mvs = dec.getMvs();
        for (int i = 0; i < 2; i++) {

            System.err.println((i == 0 ? "backRef" : "forwardRef") + ": [");
            for (int blkY = 0; blkY < mvs[i].length; ++blkY) {
                for (int blkX = 0; blkX < mvs[i][0].length; ++blkX) {
                    System.err.println("{x: " + blkX + ", y: " + blkY + ", mx: " + mvs[i][blkY][blkX][0] + ", my: "
                            + mvs[i][blkY][blkX][1] + ", ridx:" + mvs[i][blkY][blkX][2] + "},");
                }
            }
            System.err.println("],");
            if (dec.getFrameType() != SliceType.B)
                break;
        }
        System.err.println("}");
    }

    @Override
    public ColorSpace getInputColor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ColorSpace getOutputColor() {
        // TODO Auto-generated method stub
        return null;
    }
}