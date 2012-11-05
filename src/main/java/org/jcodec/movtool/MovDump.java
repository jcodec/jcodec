package org.jcodec.movtool;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jcodec.common.io.RandomAccessFileInputStream;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;

public class MovDump {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Syntax: movdump [options] <filename>");
            System.out.println("Options: -f <filename> save header to a file");
        }
        int idx = 0;
        File headerFile = null;
        while (idx < args.length) {
            if ("-f".equals(args[idx])) {
                ++idx;
                headerFile = new File(args[idx++]);
            } else
                break;
        }
        File source = new File(args[idx]);
        if (headerFile != null) {
            dumpHeader(headerFile, source);
        }

        System.out.println(print(source));
    }

    private static void dumpHeader(File headerFile, File source) throws IOException, FileNotFoundException {
        RandomAccessFileInputStream raf = null;
        DataOutputStream daos = null;
        try {
            raf = new RandomAccessFileInputStream(source);
            daos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(headerFile)));

            for (Atom atom : MP4Util.getRootAtoms(raf)) {
                String fourcc = atom.getHeader().getFourcc();
                if ("moov".equals(fourcc) || "ftyp".equals(fourcc)) {
                    atom.copy(raf, daos);
                }
            }
        } finally {
            IOUtils.closeQuietly(raf);
            IOUtils.closeQuietly(daos);
        }
    }

    public static String print(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        MP4Util.parseMovie(file).dump(sb);

        return sb.toString();
    }
}
