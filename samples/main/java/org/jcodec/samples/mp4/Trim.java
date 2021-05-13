package org.jcodec.samples.mp4;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.model.Rational;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.Flatten;
import org.jcodec.movtool.Strip;
import org.jcodec.movtool.Util;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This example trims a movie allowing to select the duration and offset of a
 * trim.
 * 
 * @author The JCodec project
 * 
 */
public class Trim {
    private static final Flag FLAG_FROM_SEC = Flag.flag("from", "f", "List of start seconds (comma separated)");
    private static final Flag FLAG_DUR_SEC = Flag.flag("duration", "d", "List of durations in seconds (comma separated)");
    private static final Flag[] flags = { FLAG_FROM_SEC, FLAG_DUR_SEC };

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        final Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpCmd("trim", flags, Arrays.asList(new String[] { "in movie", "?out movie" }));
            System.exit(-1);
            return;
        }
        File file = new File(cmd.getArg(1));
        Movie movie = MP4Util.createRefFullMovieFromFile(new File(cmd.getArg(0)));
        final double[] inS = cmd.getMultiDoubleFlagD(FLAG_FROM_SEC, new double[] {0d});
        final double[] durS = cmd.getMultiDoubleFlagD(FLAG_DUR_SEC,
                new double[] {
                        ((double) movie.getMoov().getDuration() / movie.getMoov().getTimescale()) - inS[0]
                        });
        modifyMovie(inS, durS, movie.getMoov());
        Flatten flatten = new Flatten();
        long finishTime = System.currentTimeMillis();
        System.out.println("Checkpoint: " + (finishTime - startTime) + "ms");
        flatten.flatten(movie, file);
    }

    private static void modifyMovie(double[] inS, double[] durS, MovieBox movie) throws IOException {
        for (TrakBox track : movie.getTracks()) {
            List<Edit> edits = new ArrayList<Edit>();
            for (int i = 0; i < inS.length; i++) {
                Edit edit = new Edit((long) (movie.getTimescale() * durS[i]), (long) (track.getTimescale() * inS[i]), 1f);
                edits.add(edit);
            }
            List<Edit> oldEdits = track.getEdits();
            if (oldEdits != null) {
                edits = Util.editsOnEdits(Rational.R(movie.getTimescale(), track.getTimescale()), oldEdits, edits);
            }
            track.setEdits(edits);
        }

        Strip strip = new Strip();
        strip.stripToChunks(movie);
        strip.stripToSamples(movie, false);
    }
}
