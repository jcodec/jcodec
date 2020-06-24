package org.jcodec.movtool;

import java.lang.System;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.movtool.Util.forceEditList;

import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.StringUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Cut on ref movies
 * 
 * @author The JCodec project
 * 
 */
public class Cut {
    public static void main1(String[] args) throws Exception {
        if (args.length < 1) {
            System.out
                    .println("Syntax: cut [-command arg]...[-command arg] [-self] <movie file>\n"
                            + "\tCreates a reference movie out of the file and applies a set of changes specified by the commands to it.");
            System.exit(-1);
        }

        List<Slice> slices = new ArrayList<Slice>();
        List<String> sliceNames = new ArrayList<String>();

        boolean selfContained = false;
        int shift = 0;
        while (true) {
            if ("-cut".equals(args[shift])) {
                String[] pt = StringUtils.splitS(args[shift + 1], ":");
                slices.add(new Slice(parseInt(pt[0]), parseInt(pt[1])));
                if (pt.length > 2)
                    sliceNames.add(pt[2]);
                else
                    sliceNames.add(null);
                shift += 2;
            } else if ("-self".equals(args[shift])) {
                ++shift;
                selfContained = true;
            } else
                break;
        }
        File source = new File(args[shift]);

        SeekableByteChannel input = null;
        SeekableByteChannel out = null;
        List<SeekableByteChannel> outs = new ArrayList<SeekableByteChannel>();
        try {
            input = readableChannel(source);
            Movie movie = MP4Util.createRefFullMovie(input, "file://" + source.getCanonicalPath());
            List<Movie> slicesMovs;
            if (!selfContained) {
                out = writableChannel(new File(source.getParentFile(), JCodecUtil2.removeExtension(source.getName())
                        + ".ref.mov"));
                slicesMovs = new Cut().cut(movie, slices);
                MP4Util.writeFullMovie(out, movie);
            } else {
                out = writableChannel(new File(source.getParentFile(), JCodecUtil2.removeExtension(source.getName())
                        + ".self.mov"));
                slicesMovs = new Cut().cut(movie, slices);
                new Strip().stripToChunks(movie.getMoov());
                new Flatten().flattenChannel(movie, out);
            }
            saveSlices(slicesMovs, sliceNames, source.getParentFile());
        } finally {
            if (input != null)
                input.close();
            if (out != null)
                out.close();
            for (SeekableByteChannel o : outs) {
                o.close();
            }
        }
    }

    private static void saveSlices(List<Movie> slices, List<String> names, File parentFile) throws IOException {
        for (int i = 0; i < slices.size(); i++) {
            if (names.get(i) == null)
                continue;
            SeekableByteChannel out = null;
            try {
                out = writableChannel(new File(parentFile, names.get(i)));
                MP4Util.writeFullMovie(out, slices.get(i));
            } finally {
                NIOUtils.closeQuietly(out);
            }
        }
    }

    public static class Slice {
        private double inSec;
        private double outSec;

        public Slice(double _in, double out) {
            super();
            this.inSec = _in;
            this.outSec = out;
        }
    }

    public List<Movie> cut(Movie movie, List<Slice> commands) {
        MovieBox moov = movie.getMoov();

        TrakBox videoTrack = moov.getVideoTrack();
        if (videoTrack != null && videoTrack.getTimescale() != moov.getTimescale())
            moov.fixTimescale(videoTrack.getTimescale());

        TrakBox[] tracks = moov.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrakBox trakBox = tracks[i];
            forceEditList(moov, trakBox);
            for (Slice cut : commands) {
                split(cut.inSec, moov, trakBox);
                split(cut.outSec, moov, trakBox);
            }
        }
        ArrayList<Movie> result = new ArrayList<Movie>();
        for (Slice cut : commands) {
            MovieBox clone = (MovieBox) NodeBox.cloneBox(moov, 16 * 1024 * 1024, BoxFactory.getDefault());
            for (TrakBox trakBox : clone.getTracks()) {
                selectInner(trakBox.getEdits(), cut, moov);
            }
            result.add(new Movie(movie.getFtyp(), clone));
        }

        long movDuration = 0;
        for (TrakBox trakBox : moov.getTracks()) {
            selectOuter(trakBox.getEdits(), commands, moov);
            trakBox.setEdits(trakBox.getEdits());
            movDuration = max(movDuration, trakBox.getDuration());
        }
        moov.setDuration(movDuration);

        return result;
    }

    private void selectOuter(List<Edit> edits, List<Slice> commands, MovieBox movie) {
        long[] inMv = new long[commands.size()];
        long[] outMv = new long[commands.size()];
        for (int i = 0; i < commands.size(); i++) {
            inMv[i] = (long) (commands.get(i).inSec * movie.getTimescale());
            outMv[i] = (long) (commands.get(i).outSec * movie.getTimescale());
        }
        long editStartMv = 0;
        ListIterator<Edit> lit = edits.listIterator();
        while (lit.hasNext()) {
            Edit edit = lit.next();
            for (int i = 0; i < inMv.length; i++) {
                if (editStartMv + edit.getDuration() > inMv[i] && editStartMv < outMv[i])
                    lit.remove();
            }
            editStartMv += edit.getDuration();
        }
    }

    private void selectInner(List<Edit> edits, Slice cut, MovieBox movie) {
        long inMv = (long) (movie.getTimescale() * cut.inSec);
        long outMv = (long) (movie.getTimescale() * cut.outSec);

        long editStart = 0;
        ListIterator<Edit> lit = edits.listIterator();
        while (lit.hasNext()) {
            Edit edit = lit.next();
            if (editStart + edit.getDuration() <= inMv || editStart >= outMv)
                lit.remove();
            editStart += edit.getDuration();
        }
    }

    private void split(double sec, MovieBox movie, TrakBox trakBox) {
        Util.split(movie, trakBox, (long) (sec * movie.getTimescale()));
    }
}
