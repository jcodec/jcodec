package org.jcodec.movtool;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static org.jcodec.common.JCodecUtil.removeExtension;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.containers.mp4.MP4Util.createRefMovie;
import static org.jcodec.movtool.Util.forceEditList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.jcodec.common.StringUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

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
    public static void main(String[] args) throws Exception {
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
            MovieBox movie = createRefMovie(input, "file://" + source.getCanonicalPath());
            List<MovieBox> slicesMovs;
            if (!selfContained) {
                out = writableChannel(new File(source.getParentFile(), removeExtension(source.getName())
                        + ".ref.mov"));
                slicesMovs = new Cut().cut(movie, slices);
                MP4Util.writeMovie(out, movie);
            } else {
                out = writableChannel(new File(source.getParentFile(), removeExtension(source.getName())
                        + ".self.mov"));
                slicesMovs = new Cut().cut(movie, slices);
                new Strip().strip(movie);
                new Flattern().flatternChannel(movie, out);
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

    private static void saveSlices(List<MovieBox> slices, List<String> names, File parentFile) throws IOException {
        for (int i = 0; i < slices.size(); i++) {
            if (names.get(i) == null)
                continue;
            SeekableByteChannel out = null;
            try {
                out = writableChannel(new File(parentFile, names.get(i)));
                MP4Util.writeMovie(out, slices.get(i));
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

    public List<MovieBox> cut(MovieBox movie, List<Slice> commands) {

        TrakBox videoTrack = movie.getVideoTrack();
        if (videoTrack != null && videoTrack.getTimescale() != movie.getTimescale())
            movie.fixTimescale(videoTrack.getTimescale());

        TrakBox[] tracks = movie.getTracks();
        for (TrakBox trakBox : tracks) {
            forceEditList(movie, trakBox);
            List<Edit> edits = trakBox.getEdits();
            for (Slice cut : commands) {
                split(edits, cut.inSec, movie, trakBox);
                split(edits, cut.outSec, movie, trakBox);
            }
        }
        ArrayList<MovieBox> result = new ArrayList<MovieBox>();
        for (Slice cut : commands) {
            MovieBox clone = (MovieBox) MP4Util.cloneBox(movie, 16 * 1024 * 1024);
            for (TrakBox trakBox : clone.getTracks()) {
                selectInner(trakBox.getEdits(), cut, movie, trakBox);
            }
            result.add(clone);
        }

        long movDuration = 0;
        for (TrakBox trakBox : movie.getTracks()) {
            selectOuter(trakBox.getEdits(), commands, movie, trakBox);
            trakBox.setEdits(trakBox.getEdits());
            movDuration = max(movDuration, trakBox.getDuration());
        }
        movie.setDuration(movDuration);

        return result;
    }

    private void selectOuter(List<Edit> edits, List<Slice> commands, MovieBox movie, TrakBox trakBox) {
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

    private void selectInner(List<Edit> edits, Slice cut, MovieBox movie, TrakBox trakBox) {
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

    private void split(List<Edit> edits, double sec, MovieBox movie, TrakBox trakBox) {
        Util.split(movie, trakBox, (long) (sec * movie.getTimescale()));
    }
}