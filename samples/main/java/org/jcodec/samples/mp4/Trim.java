package org.jcodec.samples.mp4;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.Flatten;
import org.jcodec.movtool.MP4Edit;
import org.jcodec.movtool.RelocateMP4Editor;
import org.jcodec.movtool.Strip;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This example trims a movie allowing to select the duration and offset of a trim.
 * 
 * @author The JCodec project
 * 
 */
public class Trim {
    private static final Flag FLAG_FROM_SEC = Flag.flag("from", "f", "From second");
    private static final Flag FLAG_TO_SEC = Flag.flag("duration", "d", "Duration of the audio");
    private static final Flag FLAG_INPLACE = Flag.flagVoid("inplace", "inpl",
            "Edit the movie in place, without copying");
    private static final Flag[] flags = { FLAG_FROM_SEC, FLAG_TO_SEC, FLAG_INPLACE };

    public static void main(String[] args) throws Exception {
        final Cmd cmd = MainUtils.parseArguments(args, flags);
        boolean inplace = cmd.hasVoidFlag(FLAG_INPLACE);
        if (inplace && cmd.argsLength() < 1 || !inplace && cmd.argsLength() < 2) {
            MainUtils.printHelpCmd("strip", flags, Arrays.asList(new String[] { "in movie", "?out movie" }));
            System.exit(-1);
            return;
        }
        SeekableByteChannel input = null;
        SeekableByteChannel out = null;

        try {
            if (!inplace) {
                input = readableChannel(new File(cmd.getArg(0)));
                File file = new File(cmd.getArg(1));
                out = writableChannel(file);
                Movie movie = MP4Util.createRefFullMovie(input, "file://" + new File(cmd.getArg(0)).getAbsolutePath());
                modifyMovie(cmd, movie.getMoov());
                new Flatten().flattenChannel(movie, out);
            } else {
                System.out.println("Modifying " + cmd.getArg(0) + " in place.");
                MP4Edit edit = new MP4Edit() {
                    @Override
                    public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
                    }

                    @Override
                    public void apply(MovieBox movie) {
                        try {
                            modifyMovie(cmd, movie);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                new RelocateMP4Editor().modifyOrRelocate(new File(cmd.getArg(0)), edit);
            }
        } finally {
            if (input != null)
                input.close();
            if (out != null)
                out.close();
        }
    }

    private static void modifyMovie(Cmd cmd, MovieBox movie) throws IOException {
        int inS = cmd.getIntegerFlagD(FLAG_FROM_SEC, 0);
        int durS = cmd.getIntegerFlagD(FLAG_TO_SEC, (int) (movie.getDuration() / movie.getTimescale()) - inS);
        for (TrakBox track : movie.getTracks()) {
            List<Edit> edits = new ArrayList<Edit>();
            Edit edit = new Edit(movie.getTimescale() * durS, track.getTimescale() * inS, 1f);
            edits.add(edit);
            track.setEdits(edits);
        }

        new Strip().strip(movie);
    }
}
