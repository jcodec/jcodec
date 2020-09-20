package org.jcodec.movtool;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.Chunk;
import org.jcodec.containers.mp4.ChunkReader;
import org.jcodec.containers.mp4.ChunkWriter;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.AliasBox;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
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
public class Flatten {
    public static interface SampleProcessor {
        ByteBuffer processSample(ByteBuffer src, double pts, double duration) throws IOException;
    }

    public static void main1(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: self <ref movie> <out movie>");
            System.exit(-1);
        }
        File outFile = new File(args[1]);
        Platform.deleteFile(outFile);
        SeekableByteChannel input = null;
        try {
            input = readableChannel(new File(args[0]));
            Movie movie = MP4Util.parseFullMovieChannel(input);
            new Flatten().flatten(movie, outFile);
        } finally {
            if (input != null)
                input.close();
        }
    }

    public List<ProgressListener> listeners;
    private Map<TrakBox, SampleProcessor> sampleProcessors = new HashMap<TrakBox, SampleProcessor>();

    public Flatten() {
        this.listeners = new ArrayList<Flatten.ProgressListener>();
    }

    public interface ProgressListener {
        public void trigger(int progress);
    }

    public void addProgressListener(ProgressListener listener) {
        this.listeners.add(listener);
    }

    public boolean setSampleProcessor(TrakBox trak, SampleProcessor processor) {
        this.sampleProcessors.put(trak, processor);
        return true;
    }

    public void flattenChannel(Movie movie, SeekableByteChannel out) throws IOException {
        MovieBox moov = movie.getMoov();

        if (!moov.isPureRefMovie())
            throw new IllegalArgumentException("movie should be reference");
        out.setPosition(0);
        MP4Util.writeFullMovie(out, movie);

        int extraSpace = calcSpaceReq(moov);
        ByteBuffer buf = ByteBuffer.allocate(extraSpace);
        out.write(buf);

        long mdatOff = out.position();
        File[] inputFiles = getInputs(moov);
        SeekableByteChannel[] inputs = new SeekableByteChannel[inputFiles.length];
        for (int i = 0; i < inputFiles.length; i++) {
            inputs[i] = NIOUtils.readableChannel(inputFiles[i]);
        }
        flattenInt(out, moov, inputs);
        long mdatSize = out.position() - mdatOff;

        out.setPosition(0);
        MP4Util.writeFullMovie(out, movie);

        long extra = mdatOff - out.position();
        if (extra < 0)
            throw new IOException("Not enough space to write the header");
        writeHeader(Header.createHeader("free", extra), out);

        out.setPosition(mdatOff);
        writeHeader(Header.createHeader("mdat", mdatSize), out);
    }

    public void flattenOnTop(Movie movie, File outF) throws IOException {
        MovieBox moov = movie.getMoov();
        if (!moov.isPureRefMovie())
            throw new IllegalArgumentException("movie should be reference");

        FileChannelWrapper out = null;
        try {
            out = NIOUtils.rwChannel(outF);
            out.setPosition(0);
            Atom atom = MP4Util.getMoov(MP4Util.getRootAtoms(out));
            long pos = atom.getOffset() + atom.getHeader().headerSize() - 4;
            out.setPosition(pos);
            out.write(ByteBuffer.wrap(new byte[] { 'f', 'r', 'e', 'e' }));

            // Go to the end
            out.setPosition(out.size());
            long mdatOff = out.position();
            File[] inputFiles = getInputs(moov);
            SeekableByteChannel[] inputs = new SeekableByteChannel[inputFiles.length];
            for (int i = 0; i < inputFiles.length; i++) {
                if (!inputFiles[i].getCanonicalPath().contentEquals(outF.getCanonicalPath())) {
                    inputs[i] = NIOUtils.readableChannel(inputFiles[i]);
                }
            }
            flattenInt(out, moov, inputs);
            long mdatSize = out.position() - mdatOff;

            MP4Util.writeMovie(out, movie.getMoov());

            out.setPosition(mdatOff);
            writeHeader(Header.createHeader("mdat", mdatSize), out);
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }

    private void flattenInt(SeekableByteChannel out, MovieBox moov, SeekableByteChannel[] inputs) throws IOException {
        writeHeader(Header.createHeader("mdat", 0x100000001L), out);

        TrakBox[] tracks = moov.getTracks();
        ChunkReader[] readers = new ChunkReader[tracks.length];
        ChunkWriter[] writers = new ChunkWriter[tracks.length];
        Chunk[] head = new Chunk[tracks.length];
        int totalChunks = 0, writtenChunks = 0, lastProgress = 0;
        long[] off = new long[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            if (inputs[i] == null)
                continue;

            readers[i] = new ChunkReader(tracks[i], inputs[i]);
            totalChunks += readers[i].size();

            writers[i] = new ChunkWriter(tracks[i], inputs[i], out);
            head[i] = readers[i].next();
            if (tracks[i].isVideo())
                off[i] = 2L * moov.getTimescale();
        }

        while (true) {
            int min = -1;
            for (int i = 0; i < readers.length; i++) {
                if (head[i] == null)
                    continue;

                if (min == -1)
                    min = i;
                else {
                    long iTv = moov.rescale(head[i].getStartTv(), tracks[i].getTimescale()) + off[i];
                    long minTv = moov.rescale(head[min].getStartTv(), tracks[min].getTimescale()) + off[min];
                    if (iTv < minTv)
                        min = i;
                }
            }
            if (min == -1)
                break;
            SampleProcessor processor = sampleProcessors.get(tracks[min]);
            if (processor != null) {
                Chunk orig = head[min];
                orig.unpackSampleSizes();
                writers[min].write(processChunk(processor, orig, tracks[min]));
                writtenChunks++;
            } else {
                writers[min].write(head[min]);
                writtenChunks++;
            }
            head[min] = readers[min].next();

            lastProgress = calcProgress(totalChunks, writtenChunks, lastProgress);
        }

        for (int i = 0; i < tracks.length; i++) {
            if (writers[i] == null) {
                ChunkWriter.cleanDrefs(tracks[i]);
            } else {
                writers[i].apply();
            }
        }
    }

    private static Chunk processChunk(SampleProcessor processor, Chunk orig, TrakBox track) throws IOException {
        ByteBuffer src = NIOUtils.duplicate(orig.getData());
        int[] sampleSizes = orig.getSampleSizes();
        int[] sampleDurs = orig.getSampleDurs();
        boolean uneqDur = orig.getSampleDur() == Chunk.UNEQUAL_DUR;
        List<ByteBuffer> modSamples = new LinkedList<ByteBuffer>();
        int totalSize = 0;
        int totalDur = 0;
        for (int ss = 0; ss < sampleSizes.length; ss++) {
            ByteBuffer sample = NIOUtils.read(src, sampleSizes[ss]);
            int sampleDur = uneqDur ? sampleDurs[ss] : orig.getSampleDur();

            ByteBuffer modSample = processor.processSample(sample,
                    ((double) (totalDur + orig.getStartTv())) / track.getTimescale(),
                    ((double) sampleDur) / track.getTimescale());
            totalDur += sampleDur;
            modSamples.add(modSample);
            totalSize += modSample.remaining();
        }
        byte[] result = new byte[totalSize];
        int[] modSizes = new int[modSamples.size()];
        int ss = 0;
        ByteBuffer tmp = ByteBuffer.wrap(result);
        for (ByteBuffer byteBuffer : modSamples) {
            modSizes[ss++] = byteBuffer.remaining();
            tmp.put(byteBuffer);
        }

        Chunk mod = Chunk.createFrom(orig);
        mod.setSampleSizes(modSizes);
        mod.setData(ByteBuffer.wrap(result));
        return mod;
    }

    private void writeHeader(Header header, SeekableByteChannel out) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(16);
        header.write(bb);
        ((java.nio.Buffer)bb).flip();
        out.write(bb);
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

    protected File[] getInputs(MovieBox movie) throws IOException {
        TrakBox[] tracks = movie.getTracks();
        File[] result = new File[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            DataRefBox drefs = NodeBox.findFirstPath(tracks[i], DataRefBox.class, Box.path("mdia.minf.dinf.dref"));
            if (drefs == null) {
                throw new IOException("No data references");
            }
            List<Box> entries = drefs.getBoxes();
            if (entries.size() != 1)
                throw new IOException("Concat tracks not supported");
            result[i] = resolveDataRef(entries.get(0));
        }
        return result;
    }

    private int calcSpaceReq(MovieBox movie) {
        int sum = 0;
        TrakBox[] tracks = movie.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrakBox trakBox = tracks[i];
            ChunkOffsetsBox stco = trakBox.getStco();
            if (stco != null)
                sum += stco.getChunkOffsets().length * 4;
        }
        return sum;
    }

    public File resolveDataRef(Box box) throws IOException {
        if (box instanceof UrlBox) {
            String url = ((UrlBox) box).getUrl();
            if (!url.startsWith("file://"))
                throw new RuntimeException("Only file:// urls are supported in data reference");
            return new File(url.substring(7));
        } else if (box instanceof AliasBox) {
            String uxPath = ((AliasBox) box).getUnixPath();
            if (uxPath == null)
                throw new RuntimeException("Could not resolve alias");
            return new File(uxPath);
        } else {
            throw new RuntimeException(box.getHeader().getFourcc() + " dataref type is not supported");
        }
    }

    public void flatten(Movie movie, File video) throws IOException {
        Platform.deleteFile(video);
        SeekableByteChannel out = null;
        try {
            out = writableChannel(video);
            flattenChannel(movie, out);
        } finally {
            if (out != null)
                out.close();
        }
    }
}
