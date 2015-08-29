package org.jcodec.samples.splitter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A wrapper for a process that 'just' executes it and returns the exit value
 * 
 * Copies of error and output streams are saved for later use
 * 
 * @author The JCodec project
 * 
 */
public class ProcessWrapper {

    private String[] command;
    private File workDir;
    private Process process;
    private ByteArrayOutputStream outCopy;
    private ByteArrayOutputStream errCopy;
    private byte[] copyBuffer = new byte[1024];

    public ProcessWrapper(String[] command, File workDir) {
        this.command = command;
        this.workDir = workDir;

        outCopy = new ByteArrayOutputStream();
        errCopy = new ByteArrayOutputStream();
    }

    public int execute() throws IOException {
        process = Runtime.getRuntime().exec(command, null, workDir);
        copyStreams();
        return process.exitValue();
    }

    public byte[] getOut() {
        return outCopy.toByteArray();
    }

    public byte[] getErr() {
        return errCopy.toByteArray();
    }

    private void copyStreams() {
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();

        while (out != null && err != null) {
            if (out != null)
                out = copyAvailableData(out, outCopy);
            if (err != null)
                err = copyAvailableData(err, errCopy);

            try {
                process.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
                // The process it not yet terminated
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    private InputStream copyAvailableData(InputStream is, OutputStream out) {
        try {
            int toRead = is.available();
            if (toRead > 0) {
                int read = 0;
                while ((read = is.read(copyBuffer, 0,
                        toRead > copyBuffer.length ? copyBuffer.length : toRead)) > 0) {
                    out.write(copyBuffer, 0, read);
                    toRead = is.available();
                    if (toRead == 0)
                        break;
                }
                if (read < 0)
                    return null;
            }
        } catch (IOException e) {
            return null;
        }
        return is;
    }
}