package org.jcodec.samples.mp4;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.Chunk;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.Flatten;
import org.jcodec.movtool.Strip;
import org.jcodec.samples.mp4.Test1Proto.Words;
import org.jcodec.samples.mp4.Test1Proto.Words.Word;
import org.jcodec.samples.mp4.Test2Proto.Tag;

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
    private static final Flag[] flags = { FLAG_FROM_SEC, FLAG_TO_SEC};

    public static void main(String[] args) throws Exception {
        final Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpCmd("strip", flags, Arrays.asList(new String[] { "in movie", "?out movie" }));
            System.exit(-1);
            return;
        }
        SeekableByteChannel input = null;
        SeekableByteChannel out = null;

        try {
            input = readableChannel(new File(cmd.getArg(0)));
            File file = new File(cmd.getArg(1));
            out = writableChannel(file);
            Movie movie = MP4Util.createRefFullMovie(input, "file://" + new File(cmd.getArg(0)).getAbsolutePath());
            final int inS = cmd.getIntegerFlagD(FLAG_FROM_SEC, 0);
            final int durS = cmd.getIntegerFlagD(FLAG_TO_SEC, (int) (movie.getMoov().getDuration() / movie.getMoov().getTimescale()) - inS);
            modifyMovie(inS, durS, movie.getMoov());
            Flatten flatten = new Flatten();
            flatten.setChunkHandler(new Flatten.ChunkHandler() {
				@Override
				public Chunk processChunk(TrakBox trak, Chunk src) throws IOException {
					return editMetadata(trak, src, inS, durS);
				}
			});
			flatten.flattenChannel(movie, out);
        } finally {
            if (input != null)
                input.close();
            if (out != null)
                out.close();
        }
    }
    
    private static Chunk editMetadata(TrakBox trak, Chunk src, int inS, int durS) throws IOException {
    	long masterOnset  = inS * 1000;
		long masterOffset = masterOnset + durS * 1000;
		if (trak.getTrackHeader().getNo() == 1) {
			ProtoReader<Words> reader = new ProtoReader<Words>(src.getData(), Words.class);
			ProtoWriter<Words> writer = new ProtoWriter<Words>(Words.class);
			int totalWords = 0;
			do {
				Words words = reader.nextProto();
				if (words == null)
					break;
				Words.Builder builder = words.toBuilder();
				int w  = 0;
				while (builder.getWordsCount() > w) {
					Word word = builder.getWords(w);
					long onset = word.getOnsetMilli();
					long offset = word.getOffsetMilli();
					if (onset > masterOnset && offset < masterOffset) {
						word = word.toBuilder().setOnsetMilli(onset - masterOnset).setOffsetMilli(offset - masterOnset).build();
						builder.setWords(w++, word);
					} else {
						builder.removeWords(w);
					}
				}
				words = builder.build();
				if (words.getWordsCount() > 0) {
					writer.writeProto(words);
					System.out.println(words);
				}
				totalWords += words.getWordsCount();
			} while(true);
			if (totalWords == 0)
				return null;
			src.setData(writer.getBytes());
		} else if (trak.getTrackHeader().getNo() == 2) {
			ProtoReader<Tag> reader = new ProtoReader<Tag>(src.getData(), Tag.class);
			ProtoWriter<Tag> writer = new ProtoWriter<Tag>(Tag.class);
			do {
				Tag audioTag = reader.nextProto();
				if (audioTag == null)
					break;
				long onset = audioTag.getOnsetMilli() - masterOnset;
				onset = onset < 0 ? 0 : onset;
				audioTag = audioTag.toBuilder().setOnsetMilli(onset).build();
				writer.writeProto(audioTag);
				System.out.println(audioTag);
			} while(true);
			src.setData(writer.getBytes());
		}
		return src;
	}

    private static void modifyMovie(int inS, int durS, MovieBox movie) throws IOException {
        for (TrakBox track : movie.getTracks()) {
            List<Edit> edits = new ArrayList<Edit>();
            Edit edit = new Edit(movie.getTimescale() * durS, track.getTimescale() * inS, 1f);
            edits.add(edit);
            track.setEdits(edits);
        }

        Strip strip = new Strip();
		strip.strip(movie);
		strip.trim(movie, "meta");
    }
    
    private static class ProtoReader<T extends com.google.protobuf.GeneratedMessageV3> {
    	private ByteBuffer src;
    	private Class<T> clz;

		public ProtoReader(ByteBuffer src, Class<T> clz) {
    		this.src = src;
    		this.clz = clz;
    	}
		
		public T nextProto() throws IOException {
			try {
				if (src.remaining() < 4)
					return null;
				int size = src.getInt();
				byte[] bytes = new byte[size];
				src.get(bytes);
				Method method = clz.getMethod("parseFrom", byte[].class);
				return (T)method.invoke(null, bytes);
			} catch(Exception e) {
				throw new IOException(e);
			}
		}
    }
    
    private static class ProtoWriter<T extends com.google.protobuf.GeneratedMessageV3> {
    	private List<byte[]> processed = new ArrayList<byte[]>();
    	private int outSize = 0;
    	private Class<T> clz;

		public ProtoWriter(Class<T> clz) {
    		this.clz = clz;
    	}
		
		public void writeProto(T proto) throws IOException {
			try {
				Method method = clz.getMethod("toByteArray");
				byte[] byteArray = (byte[])method.invoke(proto);
				processed.add(byteArray);
				outSize += 4 + byteArray.length;
			} catch(Exception e) {
				throw new IOException(e);
			}
		}
		
		public ByteBuffer getBytes() {
			byte[] outbs = new byte[outSize];
			ByteBuffer out = ByteBuffer.wrap(outbs);
			for (byte[] bs : processed) {
				out.putInt(bs.length);
				out.put(bs);
			}
			return ByteBuffer.wrap(outbs);
		}
    }
}
