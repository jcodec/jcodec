package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.RgbToBgr8Bit;
import org.jcodec.scale.Transform8Bit;

public abstract class ToImgProfile implements Profile {

	protected abstract VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) throws IOException;

	protected abstract DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException;

	protected abstract Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, Packet pkt);

	protected abstract Packet nextPacket(DemuxerTrack inTrack) throws IOException;

	protected DemuxerTrackMeta getTrackMeta(DemuxerTrack inTrack, ByteBuffer firstFrame) {
		return inTrack.getMeta();
	}

	protected void populateAdditionalFlags(HashMap<String, String> flags) {
	}

	protected boolean validateArguments(Cmd cmd) {
		return true;
	}

	@Override
	public void transcode(Cmd cmd) throws IOException {
		if (!validateArguments(cmd))
			return;
		SeekableByteChannel source = null;
		try {
			source = readableFileChannel(cmd.getArg(0));

			DemuxerTrack inTrack = getDemuxer(cmd, source);
			Picture8Bit target1 = null;

			Picture8Bit rgb = null;
			// ByteBuffer _out = ByteBuffer.allocate(videoSize.getWidth() *
			// videoSize.getHeight() * 6);
			BufferedImage bi = null;

			Transform8Bit transform = null;
			RgbToBgr8Bit rgbToBgr = new RgbToBgr8Bit();

			Packet inFrame;
			VideoDecoder decoder = null;
			int totalFrames = 0;
			for (int i = 0; (inFrame = nextPacket(inTrack)) != null; i++) {
				ByteBuffer data = inFrame.getData();

				if (decoder == null) {
					decoder = getDecoder(cmd, inTrack, data);
				}

				if (target1 == null) {
					DemuxerTrackMeta meta = getTrackMeta(inTrack, data);
					if (meta != null && meta.getDimensions() != null) {
						Size videoSize = meta.getDimensions();
						target1 = Picture8Bit.create((videoSize.getWidth() + 15) & ~0xf,
								(videoSize.getHeight() + 15) & ~0xf, ColorSpace.YUV444);
					} else {
						target1 = Picture8Bit.create(1920, 1088, ColorSpace.YUV444);
					}
					totalFrames = meta.getTotalFrames();
				}
				Picture8Bit dec = decodeFrame(decoder, target1, inFrame);
				// Some decoders might not decode some frames
				if (dec == null)
					continue;
				if (transform == null) {
					transform = ColorUtil.getTransform8Bit(dec.getColor(), ColorSpace.RGB);
					if (transform == null) {
						System.err.println(
								"Couldn't get transforem from " + dec.getColor() + " to RGB required for PNG output.");
					}
				}
				if (rgb == null) {
					rgb = Picture8Bit.create(dec.getWidth(), dec.getHeight(), ColorSpace.RGB);
					bi = new BufferedImage(dec.getWidth(), dec.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
				}
				transform.transform(dec, rgb);
				rgbToBgr.transform(rgb, rgb);
				// _out.clear();

				AWTUtil.toBufferedImage8Bit(rgb, bi);
				ImageIO.write(bi, "png", tildeExpand(format(cmd.getArg(1), i)));
				if (i % 100 == 0) {
					if (totalFrames > 0)
						System.out.print((i * 100 / totalFrames) + "%\r");
					else
						System.out.print(i + "\r");
				}
			}
		} finally {
			if (source != null)
				source.close();
		}
	}

	@Override
	public void printHelp(PrintStream err) {
		HashMap<String, String> flags = new HashMap<String, String>();
		populateAdditionalFlags(flags);
		MainUtils.printHelpVarArgs(flags, "in file", "pattern");
	}

	@Override
	public Set<Format> inputFormat() {
		return TranscodeMain.formats(Format.MOV);
	}

	@Override
	public Set<Format> outputFormat() {
		return TranscodeMain.formats(Format.IMG);
	}

	@Override
	public Set<Codec> outputVideoCodec() {
		return TranscodeMain.codecs(Codec.PNG);
	}

	@Override
	public Set<Codec> inputAudioCodec() {
		return null;
	}

	@Override
	public Set<Codec> outputAudioCodec() {
		return null;
	}
}
