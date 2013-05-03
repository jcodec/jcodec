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

    public VideoMediaHeaderBox() {
        super(new Header(fourcc()));
    }

    public VideoMediaHeaderBox(Header header) {
        super(header);
    }

    public VideoMediaHeaderBox(int graphicsMode, int rOpColor, int gOpColor, int bOpColor) {
        super(new Header(fourcc()));

        this.graphicsMode = graphicsMode;
        this.rOpColor = rOpColor;
        this.gOpColor = gOpColor;
        this.bOpColor = bOpColor;
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