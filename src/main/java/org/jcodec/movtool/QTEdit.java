package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class QTEdit {

    protected final CommandFactory[] factories;

    public static interface CommandFactory {
        String getName();

        Command parseArgs(List<String> args);

        String getHelp();
    }

    public static interface Command {
        /**
         * Performs changes on movie header
         * 
         * @param movie
         */
        void apply(MovieBox movie, SeekableByteChannel[][] refs) throws IOException;
    }

    public static abstract class BaseCommand implements Command {
        public void apply(MovieBox movie, FileChannel[][] refs) {
            apply(movie);
        }

        public abstract void apply(MovieBox movie);
    }

    public QTEdit(CommandFactory... factories) {
        this.factories = factories;
    }

    public void execute(String[] args) throws Exception {
        LinkedList<String> aa = new LinkedList<String>(Arrays.asList(args));

        final List<Command> commands = new LinkedList<Command>();
        while (aa.size() > 0) {
            int i;
            for (i = 0; i < factories.length; i++) {
                if (aa.get(0).equals(factories[i].getName())) {
                    aa.remove(0);
                    try {
                        commands.add(factories[i].parseArgs(aa));
                    } catch (Exception e) {
                        System.err.println("ERROR: " + e.getMessage());
                        return;
                    }
                    break;
                }
            }
            if (i == factories.length)
                break;
        }
        if (aa.size() == 0) {
            System.err.println("ERROR: A movie file should be specified");
            help();
        }
        if (commands.size() == 0) {
            System.err.println("ERROR: At least one command should be specified");
            help();
        }
        File input = new File(aa.remove(0));

        if (!input.exists()) {
            System.err.println("ERROR: Input file '" + input.getAbsolutePath() + "' doesn't exist");
            help();
        }

        MovieBox movie = MP4Util.createRefMovie(input);

        final SeekableByteChannel[][] inputs = new Flattern().getInputs(movie);

        applyCommands(movie, inputs, commands);

        File out = new File(input.getParentFile(), "." + input.getName());
        new Flattern() {
            protected SeekableByteChannel[][] getInputs(MovieBox movie) throws IOException {
                return inputs;
            }
        }.flattern(movie, out);

        out.renameTo(input);
    }

    private static void applyCommands(MovieBox mov, SeekableByteChannel[][] refs, List<Command> commands) throws IOException {
        for (Command command : commands) {
            command.apply(mov, refs);
        }
    }

    protected void help() {
        System.out.println("Quicktime movie editor");
        System.out.println("Syntax: qtedit <command1> <options> ... <commandN> <options> <movie>");
        System.out.println("Where options:");
        for (CommandFactory commandFactory : factories) {
            System.out.println("\t" + commandFactory.getHelp());
        }

        System.exit(-1);
    }
}