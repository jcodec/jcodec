package org.jcodec.containers.mxf.streaming;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mxf.MXFConst.MXFCodecMapping;
import org.jcodec.containers.mxf.MXFDemuxer;
import org.jcodec.containers.mxf.MXFDemuxer.MXFDemuxerTrack;
import org.jcodec.containers.mxf.MXFDemuxer.MXFPacket;
import org.jcodec.containers.mxf.read.GenericDescriptor;
import org.jcodec.containers.mxf.read.KLV;
import org.jcodec.containers.mxf.read.Track;
import org.jcodec.containers.mxf.read.UL;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;
import org.jcodec.movtool.streaming.tracks.FilePool;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A virtual track that extracts frames from MXF as it those were from MP4
 * 
 * @author The JCodec project
 * 
 */
public class MXFVirtualTrack implements VirtualTrack {
    private MXFDemuxerTrack track;
    private FilePool fp;
    private UL essenceUL;

    public MXFVirtualTrack(MXFDemuxerTrack track, FilePool fp) throws IOException {
        this.fp = fp;
        this.track = track;
        this.essenceUL = track.getEssenceUL();
    }

    public static MXFDemuxer createDemuxer(SeekableByteChannel channel) throws IOException {
        return new PatchedMXFDemuxer(channel);
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        MXFPacket nextFrame = (MXFPacket) track.nextFrame();
        if (nextFrame == null)
            return null;

        return new MXFVirtualPacket(nextFrame);
    }

    public class MXFVirtualPacket implements VirtualPacket {
        private MXFPacket pkt;

        public MXFVirtualPacket(MXFPacket pkt) {
            this.pkt = pkt;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            SeekableByteChannel ch = null;
            try {
                ch = fp.getChannel();
                ch.position(pkt.getOffset());

                KLV kl = KLV.readKL(ch);
                while (kl != null && !essenceUL.equals(kl.key)) {
                    ch.position(ch.position() + kl.len);
                    kl = KLV.readKL(ch);
                }

                return kl != null && essenceUL.equals(kl.key) ? NIOUtils.fetchFrom(ch, (int) kl.len) : null;
            } finally {
                NIOUtils.closeQuietly(ch);
            }
        }

        @Override
        public int getDataLen() throws IOException {
            return pkt.getLen();
        }

        @Override
        public double getPts() {
            return pkt.getPtsD();
        }

        @Override
        public double getDuration() {
            return pkt.getDurationD();
        }

        @Override
        public boolean isKeyframe() {
            return pkt.isKeyFrame();
        }

        @Override
        public int getFrameNo() {
            return (int) pkt.getFrameNo();
        }
    }

    @Override
    public SampleEntry getSampleEntry() {
        return toSampleEntry(track.getDescriptor());
    }

    private SampleEntry toSampleEntry(GenericDescriptor d) {
        if (track.isVideo()) {

            VideoSampleEntry se = MP4Muxer.videoSampleEntry(MP4Util.getFourcc(track.getCodec().getCodec()),
                    new Size(d.getWidth(), d.getHeight()), "JCodec");
            se.add(new PixelAspectExt(new Rational((int) ((1000 * d.getArNum() * d.getHeight()) / (d.getArDen() * d
                    .getWidth())), 1000)));

            return se;
        } else if (track.isAudio()) {
            int sampleSize = d.getBitsPerSample() >> 3;
            MXFCodecMapping codec = track.getCodec();

            return MP4Muxer.audioSampleEntry(sampleSize == 3 ? "in24" : "sowt", 0, sampleSize, d.getChannels(),
                    d.getSrNum() / d.getSrDen(), codec == MXFCodecMapping.PCM_S16BE ? Endian.BIG_ENDIAN
                            : Endian.LITTLE_ENDIAN);
        }
        throw new RuntimeException("Can't get sample entry");
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return -1;
    }

    @Override
    public void close() {
        fp.close();
    }

    public static class PatchedMXFDemuxer extends MXFDemuxer {
        public PatchedMXFDemuxer(SeekableByteChannel ch) throws IOException {
            super(ch);
        }

        @Override
        protected MXFDemuxerTrack createTrack(UL ul, Track track, GenericDescriptor descriptor) throws IOException {
            return new MXFDemuxerTrack(ul, track, descriptor) {
                @Override
                public MXFPacket readPacket(long off, int len, long pts, int timescale, int duration, int frameNo)
                        throws IOException {
                    return new MXFPacket(null, pts, timescale, duration, frameNo, true, null, off, len);
                }
            };
        }
    }

    public int getTrackId() {
        return track.getTrackId();
    }
}
