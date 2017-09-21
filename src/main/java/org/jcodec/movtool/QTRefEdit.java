package org.jcodec.movtool;
import java.lang.IllegalStateException;
import java.lang.System;


import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.movtool.QTEdit.EditFactory;

import java.io.File;
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
public class QTRefEdit {

    protected final EditFactory[] factories;

    public QTRefEdit(EditFactory... arguments) {
        this.factories = arguments;
    }

    public void execute(String[] args) throws Exception {
        LinkedList<String> aa = new LinkedList<String>(Arrays.asList(args));

        final List<MP4Edit> edits = new LinkedList<MP4Edit>();
        while (aa.size() > 0) {
            int i;
            for (i = 0; i < factories.length; i++) {
                if (aa.get(0).equals(factories[i].getName())) {
                    aa.remove(0);
                    try {
                        edits.add(factories[i].parseArgs(aa));
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
        if (edits.size() == 0) {
            System.err.println("ERROR: At least one command should be specified");
            help();
        }
        File input = new File(aa.remove(0));

        if (aa.size() == 0) {
            System.err.println("ERROR: A movie output file should be specified");
            help();
        }

        File output = new File(aa.remove(0));

        if (!input.exists()) {
            System.err.println("ERROR: Input file '" + input.getAbsolutePath() + "' doesn't exist");
            help();
        }

        if (output.exists()) {
            System.err.println("WARNING: Output file '" + output.getAbsolutePath() + "' exist, overwritting");
        }

        Movie ref = MP4Util.createRefFullMovieFromFile(input);
        new CompoundMP4Edit(edits).apply(ref.getMoov());
        MP4Util.writeFullMovieToFile(output, ref);
        System.out.println("INFO: Created reference file: " + output.getAbsolutePath());
    }

    protected void help() {
        System.out.println("Quicktime movie editor");
        System.out.println("Syntax: qtedit <command1> <options> ... <commandN> <options> <movie> <output>");
        System.out.println("Where options:");
        for (EditFactory commandFactory : factories) {
            System.out.println("\t" + commandFactory.getHelp());
        }

        System.exit(-1);
    }
}