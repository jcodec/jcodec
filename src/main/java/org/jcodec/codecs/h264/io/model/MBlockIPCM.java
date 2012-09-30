package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * IPCM macroblock of H264
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MBlockIPCM extends Macroblock {

    private int[] samplesLuma;
    private int[] samplesChroma;

    public MBlockIPCM(int[] samplesLuma, int[] samplesChroma) {
        this.samplesLuma = samplesLuma;
        this.samplesChroma = samplesChroma;
    }

    public int[] getSamplesLuma() {
        return samplesLuma;
    }

    public int[] getSamplesChroma() {
        return samplesChroma;
    }
}