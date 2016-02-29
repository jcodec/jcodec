package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VideoMediaHeaderBox extends FullBox {
    int graphicsMode;
    int rOpColor;
    int gOpColor;
    int bOpColor;

    public static String fourcc() {
        return "vmhd";
    }

    public static VideoMediaHeaderBox createVideoMediaHeaderBox(int graphicsMode, int rOpColor, int gOpColor,
            int bOpColor) {
        VideoMediaHeaderBox vmhd = new VideoMediaHeaderBox(new Header(fourcc()));
        vmhd.graphicsMode = graphicsMode;
        vmhd.rOpColor = rOpColor;
        vmhd.gOpColor = gOpColor;
        vmhd.bOpColor = bOpColor;
        return vmhd;
    }

    public VideoMediaHeaderBox(Header header) {
        super(header);
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        graphicsMode = input.getShort();
        rOpColor = input.getShort();
        gOpColor = input.getShort();
        bOpColor = input.getShort();
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putShort((short) graphicsMode);
        out.putShort((short) rOpColor);
        out.putShort((short) gOpColor);
        out.putShort((short) bOpColor);
    }

    public int getGraphicsMode() {
        return graphicsMode;
    }

    public int getrOpColor() {
        return rOpColor;
    }

    public int getgOpColor() {
        return gOpColor;
    }

    public int getbOpColor() {
        return bOpColor;
    }
}