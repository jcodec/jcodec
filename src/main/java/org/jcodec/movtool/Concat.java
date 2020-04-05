package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.BoxFactory;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Concatenates multiple identically encoded MP4 files into a single MP4 file
 * 
 * @author The JCodec project
 * 
 */
public class Concat {
    private static MainUtils.Flag[] flags = new MainUtils.Flag[] {};

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpArgs(flags, new String[] { "output", "input*" });
            return;
        }
        FileChannelWrapper out = null;
        try {
            out = NIOUtils.writableChannel(new File(cmd.getArg(0)));
            MovieBox[] mv = new MovieBox[args.length - 1];
            long[] offsets = new long[args.length - 1];
            long prevOff = 0;
            long mdatPos = 0;
            long mdatSize = 0;
            for (int i = 1; i < cmd.argsLength(); i++) {
                File file = new File(cmd.getArg(i));
                offsets[i - 1] = prevOff;
                FileChannelWrapper in = null;
                try {
                    in = NIOUtils.readableChannel(file);
                    for (Atom atom : MP4Util.getRootAtoms(in)) {
                        if ("ftyp".equals(atom.getHeader().getFourcc()) && i == 1) {
                            atom.copy(in, out);
                            offsets[i - 1] += atom.getHeader().getSize();
                        } else if ("mdat".equals(atom.getHeader().getFourcc())) {
                            if (i == 1) {
                                mdatPos = MP4Util.mdatPlaceholder(out);
                                offsets[i - 1] += 16;
                            }
                            atom.copyContents(in, out);
                            mdatSize += atom.getHeader().getBodySize();
                            offsets[i - 1] -= atom.getOffset() + atom.getHeader().headerSize();
                        } else if ("moov".equals(atom.getHeader().getFourcc())) {
                            mv[i - 1] = (MovieBox) atom.parseBox(in);
                        }
                    }
                    prevOff = out.position();
                } finally {
                    NIOUtils.closeQuietly(in);
                }
            }
            MovieBox movieBox = new Concat().concat(mv, offsets);
            MP4Util.writeMovie(out, movieBox);
            MP4Util.writeMdat(out, mdatPos, mdatSize);
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }

    public MovieBox concat(MovieBox[] movies, long[] offsets) throws IOException {
        MovieBox result = (MovieBox) NodeBox.cloneBox(movies[0], 16 * 1024 * 1024, BoxFactory.Companion.getDefault());
        int prevTracks = 0;
        long totalDuration = 0;
        for (int i = 0; i < movies.length; i++) {
            TrakBox[] tracks = movies[i].getTracks();
            if (i != 0 && prevTracks != tracks.length) {
                throw new RuntimeException("Incompatible movies. Movie " + i + " has different number of tracks ("
                        + tracks.length + " vs " + prevTracks + ").");
            }
            prevTracks = tracks.length;
            // TODO: check sample entries
            totalDuration += movies[i].getDuration();
        }

        for (int i = 0; i < prevTracks; i++) {
            offsetTrack(result, movies, offsets, i);
        }
        result.setDuration(totalDuration);
        return result;
    }

    private void offsetTrack(MovieBox result, MovieBox[] movies, long[] offsets, int index) {
        TrakBox[] rtracks = result.getTracks();
        NodeBox rstbl = (NodeBox) NodeBox.findFirstPath(rtracks[index], Box.path("mdia.minf.stbl"));
        int totalChunks = 0;
        int totalTts = 0;
        int totalSizes = 0;
        int defaultSize = 0;
        int totalCount = 0;
        int totalStsc = 0;
        long totalDuration = 0;
        for (int i = 0; i < movies.length; i++) {
            TrakBox trakBox = movies[i].getTracks()[index];
            ChunkOffsetsBox stco = trakBox.getStco();
            ChunkOffsets64Box co64 = trakBox.getCo64();
            TimeToSampleEntry[] entries = trakBox.getStts().getEntries();
            SampleSizesBox stsz = trakBox.getStsz();
            totalStsc += trakBox.getStsc().getSampleToChunk().length;
            if (stsz.getDefaultSize() != 0) {
                defaultSize = stsz.getDefaultSize();
                totalCount += stsz.getCount();
            } else {
                int[] sizes = stsz.getSizes();
                totalSizes += sizes.length;
            }
            totalTts += entries.length;
            long[] chunkOffsets = stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets();
            totalChunks += chunkOffsets.length;
            totalDuration += trakBox.getDuration();
        }
        long[] rOffsets = new long[totalChunks];
        int[] rSizes = null;
        if (defaultSize == 0)
            rSizes = new int[totalSizes];
        TimeToSampleEntry[] rTts = new TimeToSampleEntry[totalTts];
        SampleToChunkEntry[] rStsc = new SampleToChunkEntry[totalStsc];
        int lastChunks = 0;
        for (int i = 0, rc = 0, rt = 0, rs = 0, rsc = 0; i < movies.length; i++) {
            TrakBox trakBox = movies[i].getTracks()[index];
            ChunkOffsetsBox stco = trakBox.getStco();
            ChunkOffsets64Box co64 = trakBox.getCo64();
            long[] chunkOffsets = stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets();
            for (int c = 0; c < chunkOffsets.length; c++, rc++) {
                rOffsets[rc] = chunkOffsets[c] + offsets[i];
            }
            TimeToSampleEntry[] entries = trakBox.getStts().getEntries();
            for (int t = 0; t < entries.length; t++, rt++) {
                rTts[rt] = entries[t];
            }
            if (defaultSize == 0) {
                int[] sizes = trakBox.getStsz().getSizes();
                for (int s = 0; s < sizes.length; s++, rs++) {
                    rSizes[rs] = sizes[s];
                }
            }
            SampleToChunkEntry[] stscE = trakBox.getStsc().getSampleToChunk();
            for (int sc = 0; sc < stscE.length; sc++, rsc++) {
                rStsc[rsc] = stscE[sc];
                rStsc[rsc].setFirst(rStsc[rsc].getFirst() + lastChunks);
            }
            lastChunks += chunkOffsets.length;
        }
        rstbl.replace("stts", TimeToSampleBox.createTimeToSampleBox(rTts));
        rstbl.replace("stsz", defaultSize == 0 ? SampleSizesBox.createSampleSizesBox2(rSizes)
                : SampleSizesBox.createSampleSizesBox(defaultSize, totalCount));
        rstbl.replace("stsc", SampleToChunkBox.createSampleToChunkBox(rStsc));
        rstbl.removeChildren(new String[] { "stco", "co64" });
        rstbl.add(ChunkOffsets64Box.createChunkOffsets64Box(rOffsets));
        rtracks[index].setDuration(totalDuration);
    }
}
