package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.aac.AACConts;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.MPEGDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;

class Ts2mp4 implements Profile {
    @Override
    public void transcode(Cmd cmd) throws IOException {
        File fin = new File(cmd.getArg(0));
        SeekableByteChannel sink = null;
        List<SeekableByteChannel> sources = new ArrayList<SeekableByteChannel>();
        try {
            sink = writableFileChannel(cmd.getArg(1));
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MP4);

            Set<Integer> programs = MTSDemuxer.getPrograms(fin);
            MPEGDemuxer.MPEGDemuxerTrack[] srcTracks = new MPEGDemuxer.MPEGDemuxerTrack[100];
            FramesMP4MuxerTrack[] dstTracks = new FramesMP4MuxerTrack[100];
            boolean[] h264 = new boolean[100];
            Packet[] top = new Packet[100];
            int nTracks = 0;
            long minPts = Long.MAX_VALUE;
            ByteBuffer[] used = new ByteBuffer[100];
            for (Integer guid : programs) {
                SeekableByteChannel sx = readableFileChannel(cmd.getArg(0));
                sources.add(sx);
                MTSDemuxer demuxer = new MTSDemuxer(sx, guid);
                for (MPEGDemuxerTrack track : demuxer.getTracks()) {
                    srcTracks[nTracks] = track;
                    DemuxerTrackMeta meta = track.getMeta();

                    top[nTracks] = track.nextFrameWithBuffer(ByteBuffer.allocate(1920 * 1088));
                    dstTracks[nTracks] = muxer.addTrack(
                            meta.getType() == TrackType.VIDEO ? MP4TrackType.VIDEO : MP4TrackType.SOUND, 90000);
                    if (meta.getType() == TrackType.VIDEO) {
                        h264[nTracks] = true;
                    }
                    used[nTracks] = ByteBuffer.allocate(1920 * 1088);
                    if (top[nTracks].getPts() < minPts)
                        minPts = top[nTracks].getPts();
                    nTracks++;
                }
            }

            long[] prevDuration = new long[100];
            while (true) {
                long min = Integer.MAX_VALUE;
                int mini = -1;
                for (int i = 0; i < nTracks; i++) {
                    if (top[i] != null && top[i].getPts() < min) {
                        min = top[i].getPts();
                        mini = i;
                    }
                }
                if (mini == -1)
                    break;

                Packet next = srcTracks[mini].nextFrameWithBuffer(used[mini]);
                if (next != null)
                    prevDuration[mini] = next.getPts() - top[mini].getPts();
                muxPacket(top[mini], dstTracks[mini], h264[mini], minPts, prevDuration[mini]);
                used[mini] = top[mini].getData();
                used[mini].clear();
                top[mini] = next;
            }

            muxer.writeHeader();

        } finally {
            for (SeekableByteChannel sx : sources) {
                NIOUtils.closeQuietly(sx);
            }
            NIOUtils.closeQuietly(sink);
        }
    }

    private static void muxPacket(Packet packet, FramesMP4MuxerTrack dstTrack, boolean h264, long minPts, long duration)
            throws IOException {
        if (h264) {
            if (dstTrack.getEntries().size() == 0) {
                List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
                List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
                H264Utils.wipePSinplace(packet.getData(), spsList, ppsList);
                dstTrack.addSampleEntry(H264Utils.createMOVSampleEntryFromSpsPpsList(spsList, ppsList, 4));
            } else {
                H264Utils.wipePSinplace(packet.getData(), null, null);
            }
            H264Utils.encodeMOVPacket(packet.getData());
        } else {
            org.jcodec.codecs.aac.ADTSParser.Header header = ADTSParser.read(packet.getData());
            if (dstTrack.getEntries().size() == 0) {

                AudioSampleEntry ase = AudioSampleEntry.createAudioSampleEntry(Header.createHeader("mp4a", 0),
                        (short) 1, (short) AACConts.AAC_CHANNEL_COUNT[header.getChanConfig()], (short) 16,
                        AACConts.AAC_SAMPLE_RATES[header.getSamplingIndex()], (short) 0, 0, 0, 0, 0, 0, 0, 2,
                        (short) 0);

                dstTrack.addSampleEntry(ase);
                ase.add(EsdsBox.fromADTS(header));
            }
        }
        dstTrack.addFrame(MP4Packet.createMP4Packet(packet.getData(), packet.getPts() - minPts, packet.getTimescale(),
                duration, packet.getFrameNo(), packet.isKeyFrame(), packet.getTapeTimecode(), 0,
                packet.getPts() - minPts, 0));
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
            }
        }, "in file", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_TS);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
    }

    @Override
    public Set<Codec> inputAudioCodec() {
        return TranscodeMain.codecs(Codec.AAC);
    }

    @Override
    public Set<Codec> outputAudioCodec() {
        return TranscodeMain.codecs(Codec.AAC);
    }
}