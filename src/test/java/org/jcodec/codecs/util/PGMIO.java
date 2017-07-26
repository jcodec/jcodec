package org.jcodec.codecs.util;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

public class PGMIO {

    public static Picture8Bit readPGM(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        String p5 = dis.readLine();
        if (!p5.equals("P5")) {
            throw new IOException("Only P5 is supported");
        }
        String dim = dis.readLine();
        String depth = dis.readLine();

        String[] tmp = dim.split(" ");
        int width = Integer.parseInt(tmp[0]);
        int height = Integer.parseInt(tmp[1]);

        byte[] buf = new byte[width * height];
        byte[] y = new byte[width * height];

        int read = dis.read(buf, 0, width * height);

        if (read != width * height) {
            throw new IOException("Could not read data fully");
        }

        for (int i = 0; i < width * height; i++) {
            y[i] = (byte)((buf[i] & 0xff) - 128);
        }

        return Picture8Bit.createPicture8Bit(width, height, new byte[][] { y }, ColorSpace.GREY);
    }

    public static Picture8Bit readPGMFile(File name) throws IOException {
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(name));
            return readPGM(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

    }

    public static void savePGM(Picture8Bit ref, String string) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(string);
            PrintStream ps = new PrintStream(new BufferedOutputStream(out));
            ps.println("P5");
            ps.println(ref.getWidth() + " " + ref.getHeight());
            ps.println("255");
            ps.flush();
            byte[] data = ref.getPlaneData(0);
            for (int i = 0; i < data.length; i++)
                ps.write(data[i]);
            ps.flush();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
