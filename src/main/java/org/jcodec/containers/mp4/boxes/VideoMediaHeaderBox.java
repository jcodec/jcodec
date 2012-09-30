package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
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

    public VideoMediaHeaderBox(int graphicsMode,
            int rOpColor,
            int gOpColor,
            int bOpColor) {
        super(new Header(fourcc()));

        this.graphicsMode = graphicsMode;
        this.rOpColor = rOpColor;
        this.gOpColor = gOpColor;
        this.bOpColor = bOpColor;
    }

    @Override
    public void parse(InputStream input) throws IOException {
        super.parse(input);
        graphicsMode = (int) ReaderBE.readInt16(input);
        rOpColor = (int) ReaderBE.readInt16(input);
        gOpColor = (int) ReaderBE.readInt16(input);
        bOpColor = (int) ReaderBE.readInt16(input);
    }

    @Override
    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeShort(graphicsMode);
        out.writeShort(rOpColor);
        out.writeShort(gOpColor);
        out.writeShort(bOpColor);
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
