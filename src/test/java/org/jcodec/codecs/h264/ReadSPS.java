package org.jcodec.codecs.h264;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;

public class ReadSPS {

    public static void main(String[] args) throws Exception {
        printSPS(new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x81, 0x00, 0x4b, 0x60, 0x22, 0x00, 0x00, 0x07, 0xd2,
                0x00, 0x01, 0x77, 0x00, 0x1e, 0x2c, 0x5c, 0xb0 });
        printPPS(new short[] { 0xe9, 0x30, 0xb2, 0xc8, 0xb0 });

        printSPS(new short[] { 0x64, 0x00, 0x15, 0xac, 0x72, 0x14, 0x08, 0x02, 0x5b, 0x01, 0x10, 0x00, 0x00, 0x3e,
                0x90, 0x00, 0x0b, 0xb8, 0x00, 0xf1, 0x62, 0xe5, 0x80 });
        printPPS(new short[] { 0xe9, 0x33, 0x2c, 0x8b });
    }

    private static byte[] byteArray(short[] src) {
        byte[] res = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            res[i] = (byte) src[i];
        }
        return res;
    }

    private static void printSPS(short[] sps) throws IOException {

        SeqParameterSet cc = SeqParameterSet.read(new ByteArrayInputStream(byteArray(sps)));

        // Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // System.out.println(gson.toJson(cc));
    }

    private static void printPPS(short[] pps) throws IOException {

        PictureParameterSet cc = PictureParameterSet.read(new ByteArrayInputStream(byteArray(pps)));

        // Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // System.out.println(gson.toJson(cc));
    }
}
