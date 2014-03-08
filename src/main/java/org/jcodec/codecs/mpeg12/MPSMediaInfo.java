package org.jcodec.codecs.mpeg12;

import static org.jcodec.common.NIOUtils.cloneBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.Codec;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.containers.mps.MPSUtils.PESReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Gets media info from MPEG PS file
 * 
 * @author The JCodec project
 * 
 */
public class MPSMediaInfo extends PESReader {

	private Map<Integer, Stream> infos;
	private int probeLeft;
	private PSM psm;

	public class Stream {
		int streamId;
		Codec codec;
		ByteBuffer probeData;

		public Stream(int streamId) {
			this.streamId = streamId;
		}
	}

	public void getMediaInfo(File f) throws IOException {
		try {
			new NIOUtils.FileReader() {
				@Override
				protected void data(ByteBuffer data, long filePos) {
					analyseBuffer(data, filePos);
				}
			}.readFile(f, 0x10000, null);
		} catch (MediaInfoDone e) {
			System.out.println("MEDIA INFO DONE");
		}
	}

	private class MediaInfoDone extends RuntimeException {
	};

	@Override
	protected void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
		if (stream == 0xbb && infos == null) {
			infos = new HashMap<Integer, MPSMediaInfo.Stream>();
			for (int streamId : parseSystem(pesBuffer)) {
				infos.put(streamId, new Stream(streamId));
			}
			probeLeft = infos.size();
		} else if (stream == 0xbc && psm == null) {
			psm = parsePSM(pesBuffer);
		}
		if (infos != null) {
			Stream info = infos.get(stream);
			if (info != null && info.probeData == null) {
				info.probeData = cloneBuffer(pesBuffer);
				if (--probeLeft == 0) {
					deriveMediaInfo();
					throw new MediaInfoDone();
				}
			}
		}
	}

	private void deriveMediaInfo() {
		Collection<Stream> values = infos.values();
		for (Stream stream : values) {
			int streamId = 0x100 | stream.streamId;
			if (streamId >= MPSUtils.AUDIO_MIN
					&& streamId <= MPSUtils.AUDIO_MAX) {
				stream.codec = Codec.MP2;
			} else if (streamId == MPSUtils.PRIVATE_1) {
				ByteBuffer dup = stream.probeData.duplicate();
				MPSUtils.readPESHeader(dup, 0);
				int type = dup.get() & 0xff;

				if (type >= 0x80 && type <= 0x87) {
					stream.codec = Codec.AC3;
				} else if ((type >= 0x88 && type <= 0x8f)
						|| (type >= 0x98 && type <= 0x9f)) {
					stream.codec = Codec.DTS;
				} else if (type >= 0xa0 && type <= 0xaf) {
					stream.codec = Codec.PCM_DVD;
				} else if (type >= 0xb0 && type <= 0xbf) {
					stream.codec = Codec.TRUEHD;
				} else if (type >= 0xc0 && type <= 0xcf) {
					stream.codec = Codec.AC3;
				}

			} else if (streamId >= MPSUtils.VIDEO_MIN
					&& streamId <= MPSUtils.VIDEO_MAX) {
				stream.codec = Codec.MPEG2;
			}
		}
	}

	private int[] parseSystem(ByteBuffer pesBuffer) {
		NIOUtils.skip(pesBuffer, 12);
		IntArrayList result = new IntArrayList();
		while (pesBuffer.remaining() >= 3
				&& (pesBuffer.get(pesBuffer.position()) & 0x80) == 0x80) {
			result.add(pesBuffer.get() & 0xff);
			pesBuffer.getShort();
		}
		return result.toArray();
	}

	public static class PSM {

	}

	private PSM parsePSM(ByteBuffer pesBuffer) {
		pesBuffer.getInt();
		short psmLen = pesBuffer.getShort();
		if (psmLen > 1018)
			throw new RuntimeException("Invalid PSM");
		byte b0 = pesBuffer.get();
		byte b1 = pesBuffer.get();
		if ((b1 & 1) != 1)
			throw new RuntimeException("Invalid PSM");
		short psiLen = pesBuffer.getShort();
		ByteBuffer psi = NIOUtils.read(pesBuffer, psiLen & 0xffff);
		short elStreamLen = pesBuffer.getShort();
		parseElStreams(NIOUtils.read(pesBuffer, elStreamLen & 0xffff));
		int crc = pesBuffer.getInt();

		return new PSM();
	}

	private void parseElStreams(ByteBuffer buf) {
		while (buf.hasRemaining()) {
			byte streamType = buf.get();
			byte streamId = buf.get();
			short strInfoLen = buf.getShort();
			ByteBuffer strInfo = NIOUtils.read(buf, strInfoLen & 0xffff);
		}
	}

	public static void main(String[] args) throws IOException {
		new MPSMediaInfo().getMediaInfo(new File(args[0]));
	}
}