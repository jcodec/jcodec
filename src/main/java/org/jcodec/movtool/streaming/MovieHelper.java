package org.jcodec.movtool.streaming;
import js.lang.IllegalStateException;
import js.lang.System;


import static org.jcodec.containers.mp4.TrackType.SOUND;
import static org.jcodec.containers.mp4.TrackType.TIMECODE;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import org.jcodec.api.UnhandledStateException;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;
import org.jcodec.containers.mp4.boxes.ChannelBox;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ClearApertureBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.EncodedPixelBox;
import org.jcodec.containers.mp4.boxes.GenericMediaInfoBox;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MediaBox;
import org.jcodec.containers.mp4.boxes.MediaHeaderBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.ProductionApertureBox;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SoundMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TimecodeMediaInfoBox;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoMediaHeaderBox;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.movtool.streaming.VirtualMP4Movie.PacketChunk;
import org.jcodec.movtool.streaming.VirtualTrack.VirtualEdit;

import js.io.IOException;
import js.nio.ByteBuffer;
import js.util.ArrayList;
import js.util.Arrays;
import js.util.Date;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains methods to mux virtual movie tracks into real real MP4 movie header
 * 
 * @author The JCodec project
 * 
 */
public class MovieHelper {

    private static final int MEBABYTE = 1024 * 1024;

    private static int timescales[] = { 10000, 12000, 15000, 24000, 25000, 30000, 50000, 41000, 48000, 96000 };

    public static ByteBuffer produceHeader(PacketChunk[] chunks, VirtualTrack[] tracks, long dataSize, Brand brand)
            throws IOException {
        int defaultTimescale = 1000;

        ByteBuffer buf = ByteBuffer.allocate(6 * MEBABYTE);
        MovieBox movie = MovieBox.createMovieBox();

        double[] trackDurations = calcTrackDurations(chunks, tracks);
        long movieDur = calcMovieDuration(tracks, defaultTimescale, trackDurations);
        movie.add(movieHeader(movie, tracks.length, movieDur, defaultTimescale));

        for (int trackId = 0; trackId < tracks.length; trackId++) {

            // TODO: optimal timescale selection
            VirtualTrack track = tracks[trackId];
            CodecMeta codecMeta = track.getCodecMeta();

            boolean pcm = (codecMeta instanceof AudioCodecMeta) && ((AudioCodecMeta) codecMeta).isPCM();

            int trackTimescale = track.getPreferredTimescale();
            if (trackTimescale <= 0) {
                if (pcm)
                    trackTimescale = getPCMTs((AudioCodecMeta) codecMeta, chunks, trackId);
                else
                    trackTimescale = chooseTimescale(chunks, trackId);
            } else if (trackTimescale < 100) {
                trackTimescale *= 1000;
            } else if (trackTimescale < 1000) {
                trackTimescale *= 100;
            } else if (trackTimescale < 10000) {
                trackTimescale *= 10;
            }

            long totalDur = (long) (trackTimescale * trackDurations[trackId]);

            TrakBox trak = TrakBox.createTrakBox();

            Size dd = new Size(0, 0), sd = new Size(0, 0);
            if (codecMeta instanceof VideoCodecMeta) {
                VideoCodecMeta meta = (VideoCodecMeta) codecMeta;
                Rational pasp = meta.getPasp();
                if (pasp == null)
                    sd = dd = meta.getSize();
                else {
                    sd = meta.getSize();
                    dd = new Size(pasp.multiplyS(sd.getWidth()), sd.getHeight());
                }
            }
            TrackHeaderBox tkhd = TrackHeaderBox.createTrackHeaderBox(trackId + 1, movieDur, dd.getWidth(), dd.getHeight(), new Date().getTime(), new Date().getTime(), 1.0f, (short) 0, 0, new int[] { 0x10000, 0, 0, 0,
                            0x10000, 0, 0, 0, 0x40000000 });
            tkhd.setFlags(0xf);
            trak.add(tkhd);

            MediaBox media = MediaBox.createMediaBox();
            trak.add(media);
            media.add(MediaHeaderBox.createMediaHeaderBox(trackTimescale, totalDur, 0, new Date().getTime(), new Date().getTime(), 0));

            TrackType tt = (codecMeta instanceof AudioCodecMeta) ? TrackType.SOUND : TrackType.VIDEO;
            if (tt == TrackType.VIDEO) {
                NodeBox tapt = new NodeBox(new Header("tapt"));
                tapt.add(ClearApertureBox.createClearApertureBox(dd.getWidth(), dd.getHeight()));
                tapt.add(ProductionApertureBox.createProductionApertureBox(dd.getWidth(), dd.getHeight()));
                tapt.add(EncodedPixelBox.createEncodedPixelBox(sd.getWidth(), sd.getHeight()));
                trak.add(tapt);
            }

            HandlerBox hdlr = HandlerBox.createHandlerBox("mhlr", tt.getHandler(), "appl", 0, 0);
            media.add(hdlr);

            MediaInfoBox minf = MediaInfoBox.createMediaInfoBox();
            media.add(minf);
            mediaHeader(minf, tt);
            minf.add(HandlerBox.createHandlerBox("dhlr", "url ", "appl", 0, 0));
            addDref(minf);
            NodeBox stbl = new NodeBox(new Header("stbl"));
            minf.add(stbl);

            stbl.add(SampleDescriptionBox.createSampleDescriptionBox(new SampleEntry[] { toSampleEntry(codecMeta) }));
            if (pcm) {
                populateStblPCM(stbl, chunks, trackId, codecMeta);
            } else {
                populateStblGeneric(stbl, chunks, trackId, codecMeta, trackTimescale);
            }

            addEdits(trak, track, defaultTimescale, trackTimescale);

            movie.add(trak);
        }

        brand.getFileTypeBox().write(buf);
        movie.write(buf);
        Header.createHeader("mdat", dataSize).write(buf);
        buf.flip();

        return buf;
    }

    private static SampleEntry toSampleEntry(CodecMeta se) {

        Rational pasp = null;
        SampleEntry vse;
        if ("avc1".equals(se.getFourcc())) {
            vse = H264Utils.createMOVSampleEntryFromBytes(NIOUtils.toArray(se.getCodecPrivate()));
            pasp = ((VideoCodecMeta) se).getPasp();
        } else if (se instanceof VideoCodecMeta) {
            VideoCodecMeta ss = (VideoCodecMeta) se;
            pasp = ss.getPasp();
            vse = MP4Muxer.videoSampleEntry(se.getFourcc(), ss.getSize(), "JCodec");
        } else {
            AudioCodecMeta ss = (AudioCodecMeta) se;

            if (ss.isPCM()) {
                vse = MP4Muxer.audioSampleEntry(se.getFourcc(), 1, ss.getSampleSize(), ss.getChannelCount(),
                        ss.getSampleRate(), ss.getEndian());
            } else {
                vse = MP4Muxer.compressedAudioSampleEntry(se.getFourcc(), 1, ss.getSampleSize(), ss.getChannelCount(),
                        ss.getSampleRate(), ss.getSamplesPerPacket(), ss.getBytesPerPacket(), ss.getBytesPerFrame());
            }

            ChannelBox chan = ChannelBox.createChannelBox();
            AudioSampleEntry.setLabels(ss.getChannelLabels(), chan);
            vse.add(chan);
        }

        if (pasp != null)
            vse.add(PixelAspectExt.createPixelAspectExt(pasp));
        return vse;
    }

    private static int chooseTimescale(PacketChunk[] chunks, int trackId) {
        for (int ch = 0; ch < chunks.length; ch++) {
            if (chunks[ch].getTrackNo() == trackId) {
                double dur = chunks[ch].getPacket().getDuration(), min = Double.MAX_VALUE;
                int minTs = -1;
                for (int ts = 0; ts < timescales.length; ts++) {
                    double dd = timescales[ts] * dur;
                    double diff = dd - (int) dd;
                    if (diff < min) {
                        minTs = ts;
                        min = diff;
                    }
                }
                return timescales[minTs];
            }
        }
        return 0;
    }

    private static void addEdits(TrakBox trak, VirtualTrack track, int defaultTimescale, int trackTimescale) {
        VirtualEdit[] edits = track.getEdits();
        if (edits == null)
            return;
        List<Edit> result = new ArrayList<Edit>();
        for (VirtualEdit virtualEdit : edits) {
            result.add(new Edit((int) (virtualEdit.getDuration() * defaultTimescale),
                    (int) (virtualEdit.getIn() * trackTimescale), 1f));
        }
        trak.setEdits(result);
    }

    private static long calcMovieDuration(VirtualTrack[] tracks, int defaultTimescale, double[] dur) {
        long movieDur = 0;
        for (int trackId = 0; trackId < tracks.length; trackId++) {
            movieDur = Math.max(movieDur, (long) (defaultTimescale * dur[trackId]));
        }
        return movieDur;
    }

    private static double[] calcTrackDurations(PacketChunk[] chunks, VirtualTrack[] tracks) {
        double dur[] = new double[tracks.length];
        Arrays.fill(dur, -1);
        for (int chunkId = chunks.length - 1, n = 0; chunkId >= 0 && n < dur.length; chunkId--) {
            PacketChunk chunk = chunks[chunkId];
            int track = chunk.getTrackNo();
            if (dur[track] == -1) {
                dur[track] = chunk.getPacket().getPts() + chunk.getPacket().getDuration();
                ++n;
            }
        }
        return dur;
    }

    private static void populateStblGeneric(NodeBox stbl, PacketChunk[] chunks, int trackId, CodecMeta se, int timescale)
            throws IOException {
        LongArrayList stco = new LongArrayList(250 << 10);
        IntArrayList stsz = new IntArrayList(250 << 10);
        List<TimeToSampleEntry> stts = new ArrayList<TimeToSampleEntry>();
        IntArrayList stss = new IntArrayList(4 << 10);
        int prevDur = 0;
        int prevCount = -1;
        boolean allKey = true;

        List<Entry> compositionOffsets = new ArrayList<Entry>();
        long ptsEstimate = 0;
        int lastCompositionSamples = 0, lastCompositionOffset = 0;
        for (int chunkNo = 0; chunkNo < chunks.length; chunkNo++) {
            PacketChunk chunk = chunks[chunkNo];

            if (chunk.getTrackNo() == trackId) {
                stco.add(chunk.getPos());

                stsz.add(Math.max(0, chunk.getDataLen()));

                int dur = (int) Math.round(chunk.getPacket().getDuration() * timescale);
                if (dur != prevDur) {
                    if (prevCount != -1)
                        stts.add(new TimeToSampleEntry(prevCount, prevDur));
                    prevDur = dur;
                    prevCount = 0;
                }
                ++prevCount;

                boolean key = chunk.getPacket().isKeyframe();
                allKey &= key;
                if (key)
                    stss.add(chunk.getPacket().getFrameNo() + 1);

                long pts = Math.round(chunk.getPacket().getPts() * timescale);

                int compositionOffset = (int) (pts - ptsEstimate);
                if (compositionOffset != lastCompositionOffset) {
                    if (lastCompositionSamples > 0)
                        compositionOffsets.add(new Entry(lastCompositionSamples, lastCompositionOffset));
                    lastCompositionOffset = compositionOffset;
                    lastCompositionSamples = 0;
                }
                lastCompositionSamples++;
                ptsEstimate += dur;
            }
        }
        if (compositionOffsets.size() > 0) {
            compositionOffsets.add(new Entry(lastCompositionSamples, lastCompositionOffset));
        }

        if (prevCount > 0)
            stts.add(new TimeToSampleEntry(prevCount, prevDur));

        if (!allKey)
            stbl.add(SyncSamplesBox.createSyncSamplesBox(stss.toArray()));

        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(stco.toArray()));
        stbl.add(SampleToChunkBox.createSampleToChunkBox(new SampleToChunkEntry[] { new SampleToChunkEntry(1, 1, 1) }));
        stbl.add(SampleSizesBox.createSampleSizesBox2(stsz.toArray()));
        stbl.add(TimeToSampleBox.createTimeToSampleBox(stts.toArray(new TimeToSampleEntry[0])));
        compositionOffsets(compositionOffsets, stbl);
    }

    private static void compositionOffsets(List<Entry> compositionOffsets, NodeBox stbl) {
        if (compositionOffsets.size() > 0) {
            int min = FramesMP4MuxerTrack.minOffset(compositionOffsets);
            for (Entry entry : compositionOffsets) {
                entry.offset -= min;
            }
            stbl.add(CompositionOffsetsBox.createCompositionOffsetsBox(compositionOffsets.toArray(new Entry[0])));
        }
    }

    private static void populateStblPCM(NodeBox stbl, PacketChunk[] chunks, int trackId, CodecMeta se)
            throws IOException {
        AudioCodecMeta ase = (AudioCodecMeta) se;
        int frameSize = ase.getFrameSize();

        LongArrayList stco = new LongArrayList(250 << 10);
        List<SampleToChunkEntry> stsc = new ArrayList<SampleToChunkEntry>();
        int stscCount = -1, stscFirstChunk = -1, totalFrames = 0;

        for (int chunkNo = 0, stscCurChunk = 1; chunkNo < chunks.length; chunkNo++) {
            PacketChunk chunk = chunks[chunkNo];

            if (chunk.getTrackNo() == trackId) {
                stco.add(chunk.getPos());

                int framesPerChunk = chunk.getDataLen() / frameSize;
                if (framesPerChunk != stscCount) {
                    if (stscCount != -1)
                        stsc.add(new SampleToChunkEntry(stscFirstChunk, stscCount, 1));
                    stscFirstChunk = stscCurChunk;
                    stscCount = framesPerChunk;
                }
                stscCurChunk++;
                totalFrames += framesPerChunk;
            }
        }

        if (stscCount != -1)
            stsc.add(new SampleToChunkEntry(stscFirstChunk, stscCount, 1));

        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(stco.toArray()));
        stbl.add(SampleToChunkBox.createSampleToChunkBox(stsc.toArray(new SampleToChunkEntry[0])));
        stbl.add(SampleSizesBox.createSampleSizesBox(ase.getFrameSize(), totalFrames));
        stbl.add(TimeToSampleBox.createTimeToSampleBox(new TimeToSampleEntry[] { new TimeToSampleEntry(totalFrames, 1) }));
    }

    private static int getPCMTs(AudioCodecMeta se, PacketChunk[] chunks, int trackId) throws IOException {
        for (int chunkNo = 0; chunkNo < chunks.length; chunkNo++) {
            if (chunks[chunkNo].getTrackNo() == trackId) {
                return (int) Math.round(chunks[chunkNo].getDataLen()
                        / (se.getFrameSize() * chunks[chunkNo].getPacket().getDuration()));
            }
        }
        throw new RuntimeException("Crap");
    }

    private static void mediaHeader(MediaInfoBox minf, TrackType type) {
        if (VIDEO == type) {
            VideoMediaHeaderBox vmhd = VideoMediaHeaderBox.createVideoMediaHeaderBox(0, 0, 0, 0);
            vmhd.setFlags(1);
            minf.add(vmhd);
        } else if(SOUND == type) {
            SoundMediaHeaderBox smhd = SoundMediaHeaderBox.createSoundMediaHeaderBox();
            smhd.setFlags(1);
            minf.add(smhd);
        } else if(TIMECODE == type) {
            NodeBox gmhd = new NodeBox(new Header("gmhd"));
            gmhd.add(GenericMediaInfoBox.createGenericMediaInfoBox());
            NodeBox tmcd = new NodeBox(new Header("tmcd"));
            gmhd.add(tmcd);
            tmcd.add(TimecodeMediaInfoBox
                    .createTimecodeMediaInfoBox((short) 0, (short) 0, (short) 12, new short[] { 0, 0, 0 }, new short[] {
                            0xff, 0xff, 0xff }, "Lucida Grande"));
            minf.add(gmhd);
        } else {
            throw new UnhandledStateException("Handler " + type.getHandler() + " not supported");
        }
    }

    private static void addDref(NodeBox minf) {
        DataInfoBox dinf = DataInfoBox.createDataInfoBox();
        minf.add(dinf);
        DataRefBox dref = DataRefBox.createDataRefBox();
        dinf.add(dref);
        dref.add(LeafBox.createLeafBox(Header.createHeader("alis", 0), ByteBuffer.wrap(new byte[] { 0, 0, 0, 1 })));
    }

    private static MovieHeaderBox movieHeader(NodeBox movie, int nTracks, long duration, int timescale) {

        return MovieHeaderBox.createMovieHeaderBox(timescale, duration, 1.0f, 1.0f, new Date().getTime(), new Date().getTime(), new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 }, nTracks + 1);
    }
}