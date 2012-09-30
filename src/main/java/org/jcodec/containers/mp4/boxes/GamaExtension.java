package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.io.ReaderBE.readInt32;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * 
 * @author The JCodec project
 * 
 */
public class GamaExtension extends Box {

    private float gamma;

    public GamaExtension(float gamma) {
        super(new Header(fourcc(), 0));
        this.gamma = gamma;
    }

    public GamaExtension(Header header) {
        super(header);
    }

    public GamaExtension(Box other) {
        super(other);
    }

    public void parse(InputStream input) throws IOException {
        float g = readInt32(input);
        gamma = g / 65536f;
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeInt((int) (gamma * 65536));
    }
    
    public float getGamma(){
        return gamma;
    }

    public static String fourcc() {
        return "gama";
    }
}