package org.jcodec.codecs.h264.io.read;

import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.MBlockIPCM;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * IPCM macroblock reader
 * 
 * @author Jay Codec
 * 
 */
public class IPCMMblockReader {

    private ChromaFormat chromaFormat;
    private int bitDepthLuma;
    private int bitDepthChroma;

    public IPCMMblockReader(ChromaFormat chromaFormat, int bitDepthLuma, int bitDepthChroma) {

        this.chromaFormat = chromaFormat;
        this.bitDepthLuma = bitDepthLuma;
        this.bitDepthChroma = bitDepthChroma;
    }

    public MBlockIPCM readMBlockIPCM(InBits reader) throws IOException {
        reader.align();

        int[] samplesLuma = new int[256];
        for (int i = 0; i < 256; i++) {
            samplesLuma[i] = reader.readNBit(bitDepthLuma);
        }
        int MbWidthC = 16 / chromaFormat.getSubWidth();
        int MbHeightC = 16 / chromaFormat.getSubHeight();

        int[] samplesChroma = new int[2 * MbWidthC * MbHeightC];
        for (int i = 0; i < 2 * MbWidthC * MbHeightC; i++) {
            samplesChroma[i] = reader.readNBit(bitDepthChroma);
        }

        return new MBlockIPCM(samplesLuma, samplesChroma);
    }
}
