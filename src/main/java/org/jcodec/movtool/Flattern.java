package org.jcodec.movtool;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
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
import org.jcodec.platform.Platform;

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
        Platform.deleteFile(outFile);
        SeekableByteChannel input = null;
        try {
            input = readableChannel(new File(args[0]));
            MovieBox movie = MP4Util.parseMovieChannel(input);
            new Flattern().flattern(movie, outFile);
        } finally {
            if (input != null)
                input.close();
        }
    }

    public List<ProgressListener> listeners;
    
    public Flattern() {
        this.listeners = new ArrayList<Flattern.ProgressListener>();
    }

    public interface ProgressListener {
        public void trigger(int progress);
    }

    public void addProgressListener(ProgressListener listener) {
        this.listeners.add(listener);
    }

    public void flattern(MovieBox movie, SeekableByteChannel out) throws IOException {
        if (!movie.isPureRefMovie(movie))
            throw new IllegalArgumentException("movie should be reference");
        ByteBuffer buf = ByteBuffer.allocate(16 * 1024 * 1024);
        FileTypeBox ftyp = FileTypeBox.createFileTypeBox("qt  ", 0x20050300, Arrays.asList(new String[] { "qt  " }));
        ftyp.write(buf);
        long movieOff = buf.position();
        movie.write(buf);

        int extraSpace = calcSpaceReq(movie);
        new Header("free", 8 + extraSpace).write(buf);
        NIOUtils.skip(buf, extraSpace);

        long mdatOff = buf.position();
        new Header("mdat", 0x100000001L).write(buf);
        buf.flip();
        out.write(buf);

        SeekableByteChannel[][] inputs = getInputs(movie);

        TrakBox[] tracks = movie.getTracks();
        ChunkReader[] readers = new ChunkReader[tracks.length];
        ChunkWriter[] writers = new ChunkWriter[tracks.length];
        Chunk[] head = new Chunk[tracks.length];
        int totalChunks = 0, writtenChunks = 0, lastProgress = 0;
        long[] off = new long[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            readers[i] = new ChunkReader(tracks[i]);
            totalChunks += readers[i].size();

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
            writtenChunks++;

            lastProgress = calcProgress(totalChunks, writtenChunks, lastProgress);
        }
        long mdatSize = out.position() - mdatOff;

        for (int i = 0; i < tracks.length; i++) {
            writers[i].apply();
        }
        out.setPosition(movieOff);
        MP4Util.writeMovie(out, movie);

        long extra = mdatOff - out.position();
        if (extra < 0)
            throw new RuntimeException("Not enough space to write the header");
        out.write((ByteBuffer) ByteBuffer.allocate(8).putInt((int) extra).put(new byte[] { 'f', 'r', 'e', 'e' }).flip());

        out.setPosition(mdatOff + 8);
        out.write(ByteBuffer.allocate(8).putLong(mdatSize));
    }

    private int calcProgress(int totalChunks, int writtenChunks, int lastProgress) {
        int curProgress = 100 * writtenChunks / totalChunks;
        if (lastProgress < curProgress) {
            lastProgress = curProgress;
            for (ProgressListener pl : this.listeners)
                pl.trigger(lastProgress);
        }
        return lastProgress;
    }

    protected SeekableByteChannel[][] getInputs(MovieBox movie) throws IOException {
        TrakBox[] tracks = movie.getTracks();
        SeekableByteChannel[][] result = new SeekableByteChannel[tracks.length][];
        for (int i = 0; i < tracks.length; i++) {
            DataRefBox drefs = NodeBox.findFirst(tracks[i], DataRefBox.class, "mdia", "minf", "dinf", "dref");
            if (drefs == null) {
                throw new RuntimeException("No data references");
            }
            List<Box> entries = drefs.getBoxes();
            SeekableByteChannel[] e = new SeekableByteChannel[entries.size()];
            SeekableByteChannel[] inputs = new SeekableByteChannel[entries.size()];
            for (int j = 0; j < e.length; j++) {
                inputs[j] = resolveDataRef(entries.get(j));
            }
            result[i] = inputs;
        }
        return result;
    }

    private int calcSpaceReq(MovieBox movie) {
        int sum = 0;
        for (TrakBox trakBox : movie.getTracks()) {
            ChunkOffsetsBox stco = trakBox.getStco();
            if (stco != null)
                sum += stco.getChunkOffsets().length * 4;
        }
        return sum;
    }

    public SeekableByteChannel resolveDataRef(Box box) throws IOException {
        if (box instanceof UrlBox) {
            String url = ((UrlBox) box).getUrl();
            if (!url.startsWith("file://"))
                throw new RuntimeException("Only file:// urls are supported in data reference");
            return readableChannel(new File(url.substring(7)));
        } else if (box instanceof AliasBox) {
            String uxPath = ((AliasBox) box).getUnixPath();
            if (uxPath == null)
                throw new RuntimeException("Could not resolve alias");
            return readableChannel(new File(uxPath));
        } else {
            throw new RuntimeException(box.getHeader().getFourcc() + " dataref type is not supported");
        }
    }

    public void flattern(MovieBox movie, File video) throws IOException {
        Platform.deleteFile(video);
        SeekableByteChannel out = null;
        try {
            out = writableChannel(video);
            flattern(movie, out);
        } finally {
            if (out != null)
                out.close();
        }
    }
}