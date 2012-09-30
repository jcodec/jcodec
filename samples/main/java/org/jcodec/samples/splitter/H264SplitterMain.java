package org.jcodec.samples.splitter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Splits H264 bitstream into slices having n IDR sequences each
 * 
 * @author Jay Codec
 * 
 */
public class H264SplitterMain extends H264SplitterBase {

    private String fnPattern;

    private String slicePath;
    private String[] exec;
    private boolean verbose;
    private int maxIdr;

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("num-idr").withDescription(
                "Maximum number of IDR sequences to be written into one slice. Default is 1.").hasArg().withArgName(
                "number").create("n"));
        options.addOption(OptionBuilder.withLongOpt("pattern").withDescription(
                "File name pattern for output slices."
                        + " Use '{index}' as a placeholder for slice index."
                        + " Default is 'output{index}.264'.").hasArg().withArgName(
                "pattern").create("p"));
        options.addOption("v", "verbose", false, "Generate verbose output.");
        options.addOption(OptionBuilder.withLongOpt("exec").withDescription(
                "A command to execute after a new slice is created."
                        + " A full path to the new slice file is"
                        + " passed as a first parameter to the command."
                        + " This should be a full path to executable module."
                        + " Use shell to execute a script.").hasArg().withArgName(
                "command").create("e"));

        options.addOption("h", "help", false, "Print this message.");

        try {
            CommandLine line = parser.parse(options, args);

            String[] files = line.getArgs();
            if (line.hasOption("help") || files.length == 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("split264", options);
                System.exit(0);
            }

            String filePattern = "output{index}.264";
            if (line.hasOption("p")) {
                filePattern = line.getOptionValue("p");
            }

            File file = new File(produceFilePath(filePattern, 0));
            if (!file.isAbsolute()) {
                String fileName = files[0];
                filePattern = new File(new File(fileName).getParentFile(),
                        filePattern).getAbsolutePath();
            }

            int numIDR = 1;
            if (line.hasOption("n")) {
                numIDR = Integer.parseInt(line.getOptionValue("n"));
            }

            String execute = null;
            if (line.hasOption("e")) {
                execute = line.getOptionValue("e");
            }

            boolean verbose = false;
            if (line.hasOption("v")) {
                verbose = true;
            }

            new H264SplitterMain().doTheJob(filePattern, numIDR, execute,
                    verbose, files[0]);
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }
    }

    private void doTheJob(String filePattern, int numIDR, String execute,
            boolean verbose2, String fileName) throws Exception {
        fnPattern = filePattern;
        maxIdr = numIDR;
        if (execute != null) {
            exec = execute.split("\\s");
        }
        verbose = verbose2;
        InputStream io = null;
        try {
            String inFileName = fileName;
            if ("-".equals(inFileName)) {
                io = new BufferedInputStream(System.in);
            } else {
                io = new BufferedInputStream(new FileInputStream(inFileName));
            }

            split(io);
        } finally {
            io.close();
        }
    }

    @Override
    protected void startNewSlice() throws FileNotFoundException, IOException {
        slicePath = produceFilePath(fnPattern, getSliceCount());
        String slicePathTmp = slicePath + ".tmp";

        setOutputStream(new BufferedOutputStream(new FileOutputStream(
                slicePathTmp)));
    }

    @Override
    protected void finishCurrentSlice() {
        try {
            OutputStream os = getOutputStream();
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (slicePath != null) {
            String slicePathTmp = slicePath + ".tmp";
            File file = new File(slicePathTmp);
            file.renameTo(new File(slicePath));

            executeCommand(slicePath);
        }
    }

    private void executeCommand(String slicePath) {
        if (exec == null)
            return;

        try {
            String[] newArr = new String[exec.length + 1];
            System.arraycopy(exec, 0, newArr, 0, exec.length);
            newArr[exec.length] = slicePath;
            ProcessWrapper pw = new ProcessWrapper(newArr, null);
            int exit = pw.execute();

            if (verbose)
                System.out.println(new String(pw.getOut()));

            byte[] err = pw.getErr();
            if (err.length > 0) {
                System.err.println(new String(err));
            }

            if (exit != 0) {
                System.err.println("Command '" + join(newArr)
                        + "' exited with code: " + exit);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String join(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private static String produceFilePath(String pattern, int sliceCount) {
        return pattern.replace("{index}", String.valueOf(sliceCount));
    }

    @Override
    protected int getMaxIdr() {
        return maxIdr;
    }
}