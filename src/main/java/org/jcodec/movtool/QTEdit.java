package org.jcodec.movtool;
import java.lang.IllegalStateException;
import java.lang.System;


import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.Flattern.ProgressListener;

import java.io.File;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class QTEdit {

    protected final EditFactory[] factories;
    private final List<ProgressListener> listeners;

    public static interface EditFactory {
        String getName();

        MP4Edit parseArgs(List<String> args);

        String getHelp();
    }

    public static abstract class BaseCommand implements MP4Edit {
        public void applyRefs(MovieBox movie, FileChannel[][] refs) {
            apply(movie);
        }

        public abstract void apply(MovieBox movie);
    }

    public QTEdit(EditFactory... arguments) {
        this.listeners = new ArrayList<ProgressListener>();
        this.factories = arguments;
    }

    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    public void execute(String[] args) throws Exception {
        LinkedList<String> aa = new LinkedList<String>(Arrays.asList(args));

        final List<MP4Edit> commands = new LinkedList<MP4Edit>();
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

        new ReplaceMP4Editor().replace(input, new CompoundMP4Edit(commands));
    }

    protected void help() {
        System.out.println("Quicktime movie editor");
        System.out.println("Syntax: qtedit <command1> <options> ... <commandN> <options> <movie>");
        System.out.println("Where options:");
        for (EditFactory commandFactory : factories) {
            System.out.println("\t" + commandFactory.getHelp());
        }

        System.exit(-1);
    }
}