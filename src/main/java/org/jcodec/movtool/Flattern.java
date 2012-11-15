package org.jcodec.movtool;

import static org.jcodec.common.JCodecUtil.bufin;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.containers.mp4.Chunk;
import org.jcodec.containers.mp4.ChunkReader;
import org.jcodec.containers.mp4.ChunkWriter;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.AliasBox;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.FileTypeBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.UrlBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Self contained movie creator
 * 
 * @author The JCodec project
 * 
 */
public class Flattern {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: self <ref movie> <out movie>");
            System.exit(-1);
        }
        File outFile = new File(args[1]);
        outFile.delete();
        RAInputStream input = null;
        try {
            input = bufin(new File(args[0]));
            MovieBox movie = MP4Util.parseMovie(input);
            new Flattern().flattern(movie, outFile);
        } finally {
            if (input != null)
                input.close();
        }
    }

    public void flattern(MovieBox movie, RandomAccessFile out) throws IOException {
        if (!movie.isPureRefMovie(movie))
            throw new IllegalArgumentException("movie should be reference");
        FileTypeBox ftyp = new FileTypeBox("qt  ", 0x20050300, Arrays.asList(new String[] { "qt  " }));
        ftyp.write(out);
        long movieOff = out.getFilePointer();
        movie.write(out);

        int extraSpace = calcSpaceReq(movie);
        new Header("free", 8 + extraSpace).write(out);
        out.write(new byte[extraSpace]);

        long mdatOff = out.getFilePointer();
        new Header("mdat", 0x100000001L).write(out);

        RAInputStream[][] inputs = getInputs(movie);

        TrakBox[] tracks = movie.getTracks();
        ChunkReader[] readers = new ChunkReader[tracks.length];
        ChunkWriter[] writers = new ChunkWriter[tracks.length];
        Chunk[] head = new Chunk[tracks.length];
        long[] off = new long[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            readers[i] = new ChunkReader(tracks[i]);
            writers[i] = new ChunkWriter(tracks[i], inputs[i], out);
            head[i] = readers[i].next();
            if (tracks[i].isVideo())
                off[i] = 2 * movie.getTimescale();
        }

        while (true) {
            int min = -1;
            for (int i = 0; i < readers.length; i++) {
                if (head[i] == null)
                    continue;

                if (min == -1)
                    min = i;
                else {
                    long iTv = movie.rescale(head[i].getStartTv(), tracks[i].getTimescale()) + off[i];
                    long minTv = movie.rescale(head[min].getStartTv(), tracks[min].getTimescale()) + off[min];
                    if (iTv < minTv)
                        min = i;
                }
            }
            if (min == -1)
                break;
            writers[min].write(head[min]);
            head[min] = readers[min].next();
        }
        long mdatSize = out.getFilePointer() - mdatOff;

        for (int i = 0; i < tracks.length; i++) {
            writers[i].apply();
        }
        out.seek(movieOff);
        movie.write(out);

        long extra = mdatOff - out.getFilePointer();
        if (extra < 0)
            throw new RuntimeException("Not enough space to write the header");
        new Header("free", extra).write(out);

        out.seek(mdatOff + 8);
        out.writeLong(mdatSize);
    }

    protected RAInputStream[][] getInputs(MovieBox movie) throws IOException {
        TrakBox[] tracks = movie.getTracks();
        RAInputStream[][] result = new RAInputStream[tracks.length][];
        for (int i = 0; i < tracks.length; i++) {
            DataRefBox drefs = NodeBox.findFirst(tracks[i], DataRefBox.class, "mdia", "minf", "dinf", "dref");
            if (drefs == null) {
                throw new RuntimeException("No data references");
            }
            List<Box> entries = drefs.getBoxes();
            RAInputStream[] e = new RAInputStream[entries.size()];
            RAInputStream[] inputs = new RAInputStream[entries.size()];
            for (int j = 0; j < e.length; j++) {
                inputs[j] = Flattern.resolveDataRef(entries.get(j));
            }
            result[i] = inputs;
        }
        return result;
    }

    private int calcSpaceReq(MovieBox movie) {
        int sum = 0;
        for (TrakBox trakBox : movie.getTracks()) {
            ChunkOffsetsBox stco = Box.findFirst(trakBox, ChunkOffsetsBox.class, "mdia", "minf", "stbl", "stco");
            if (stco != null)
                sum += stco.getChunkOffsets().length * 4;
        }
        return sum;
    }

    public static RAInputStream resolveDataRef(Box box) throws IOException {
        if (box instanceof UrlBox) {
            String url = ((UrlBox) box).getUrl();
            if (!url.startsWith("file://"))
                throw new RuntimeException("Only file:// urls are supported in data reference");
            return bufin(new File(url.substring(7)));
        } else if (box instanceof AliasBox) {
            String uxPath = ((AliasBox) box).getUnixPath();
            if (uxPath == null)
                throw new RuntimeException("Could not resolve alias");
            return bufin(new File(uxPath));
        } else {
            throw new RuntimeException(box.getHeader().getFourcc() + " dataref type is not supported");
        }
    }

    public void flattern(MovieBox movie, File video) throws IOException {
        video.delete();
        RandomAccessFile out = null;
        try {
            out = new RandomAccessFile(video, "rw");
            flattern(movie, out);
        } finally {
            if (out != null)
                out.close();
        }
    }
}