package org.jcodec.common;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class IOUtils {

    public static final int DEFAULT_BUFFER_SIZE = 4096;

    public static void closeQuietly(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (IOException e) {
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static int copyDumb(InputStream input, OutputStream output) throws IOException {
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read())) {
            output.write(n);
            count++;
        }
        return count;
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        return NIOUtils.toArray(NIOUtils.fetchFrom(file));
    }

    public static String toString(InputStream is) throws IOException {
        return new String(toByteArray(is));
    }

    public static void writeStringToFile(File file, String str) throws IOException {
        NIOUtils.writeTo(ByteBuffer.wrap(str.getBytes()), file);
    }

    public static void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message = "File " + directory + " exists and is "
                        + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory()) {
                    String message = "Unable to create directory " + directory;
                    throw new IOException(message);
                }
            }
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannelWrapper in = null;
        FileChannelWrapper out = null;
        try {
            in = NIOUtils.readableFileChannel(src);
            out = NIOUtils.writableFileChannel(dst);
            NIOUtils.copy(in, out, Long.MAX_VALUE);
        } finally {
            NIOUtils.closeQuietly(in);
            NIOUtils.closeQuietly(out);
        }
    }
}
