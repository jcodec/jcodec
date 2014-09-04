package org.jcodec.movtool.streaming;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ClearApertureBox;
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.EncodedPixelBox;
import org.jcodec.containers.mp4.boxes.GenericMediaInfoBox;
import org.jcodec.containers.mp4.boxes.HandlerBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.LeafBox;
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
import org.jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SoundMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TimecodeMediaInfoBox;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.movtool.streaming.VirtualMovie.PacketChunk;
import org.jcodec.movtool.streaming.VirtualTrack.VirtualEdit;

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
        MovieBox movie = new MovieBox();

        double[] trackDurations = calcTrackDurations(chunks, tracks);
        long movieDur = calcMovieDuration(tracks, defaultTimescale, trackDurations);
        movie.add(movieHeader(movie, tracks.length, movieDur, defaultTimescale));

        for (int trackId = 0; trackId < tracks.length; trackId++) {

            // TODO: optimal timescale selection
            VirtualTrack track = tracks[trackId];
            SampleEntry se = track.getSampleEntry();

            boolean pcm = (se instanceof AudioSampleEntry) && ((AudioSampleEntry) se).isPCM();
            int trackTimescale = track.getPreferredTimescale();
            if (trackTimescale <= 0) {
                if (pcm)
                    trackTimescale = getPCMTs((AudioSampleEntry) se, chunks, trackId);
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

            TrakBox trak = new TrakBox();

            Size dd = new Size(0, 0), sd = new Size(0, 0);
            if (se instanceof VideoSampleEntry) {
                VideoSampleEntry vse = (VideoSampleEntry) se;
                PixelAspectExt pasp = Box.findFirst(vse, PixelAspectExt.class, "pasp");
                if (pasp == null)
                    sd = dd = new Size(vse.getWidth(), vse.getHeight());
                else {
                    Rational r = pasp.getRational();
                    dd = new Size(r.multiplyS(vse.getWidth()), vse.getHeight());
                    sd = new Size(vse.getWidth(), vse.getHeight());
                }
            }
            TrackHeaderBox tkhd = new TrackHeaderBox(trackId + 1, movieDur, dd.getWidth(), dd.getHeight(),
                    new Date().getTime(), new Date().getTime(), 1.0f, (short) 0, 0, new int[] { 0x10000, 0, 0, 0,
                            0x10000, 0, 0, 0, 0x40000000 });
            tkhd.setFlags(0xf);
            trak.add(tkhd);

            MediaBox media = new MediaBox();
            trak.add(media);
            media.add(new MediaHeaderBox(trackTimescale, totalDur, 0, new Date().getTime(), new Date().getTime(), 0));

            TrackType tt = (se instanceof AudioSampleEntry) ? TrackType.SOUND : TrackType.VIDEO;
            if (tt == TrackType.VIDEO) {
                NodeBox tapt = new NodeBox(new Header("tapt"));
                tapt.add(new ClearApertureBox(dd.getWidth(), dd.getHeight()));
                tapt.add(new ProductionApertureBox(dd.getWidth(), dd.getHeight()));
                tapt.add(new EncodedPixelBox(sd.getWidth(), sd.getHeight()));
                trak.add(tapt);
            }

            HandlerBox hdlr = new HandlerBox("mhlr", tt.getHandler(), "appl", 0, 0);
            media.add(hdlr);

            MediaInfoBox minf = new MediaInfoBox();
            media.add(minf);
            mediaHeader(minf, tt);
            minf.add(new HandlerBox("dhlr", "url ", "appl", 0, 0));
            addDref(minf);
            NodeBox stbl = new NodeBox(new Header("stbl"));
            minf.add(stbl);

            stbl.add(new SampleDescriptionBox(se));
            if (pcm) {
                populateStblPCM(stbl, chunks, trackId, se);
            } else {
                populateStblGeneric(stbl, chunks, trackId, se, trackTimescale);
            }

            addEdits(trak, track, defaultTimescale, trackTimescale);

            movie.add(trak);
        }

        brand.getFileTypeBox().write(buf);
        movie.write(buf);
        new Header("mdat", dataSize).write(buf);
        buf.flip();

        return buf;
    }

    private static int chooseTimescale(PacketChunk[] chunks, int trackId) {
        for (PacketChunk chunk : chunks) {
            if (chunk.getTrack() == trackId) {
                double dur = chunk.getPacket().getDuration(), min = Double.MAX_VALUE;
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
            int track = chunk.getTrack();
            if (dur[track] == -1) {
                dur[track] = chunk.getPacket().getPts() + chunk.getPacket().getDuration();
                ++n;
            }
        }
        return dur;
    }

    private static void populateStblGeneric(NodeBox stbl, PacketChunk[] chunks, int trackId, SampleEntry se,
            int timescale) throws IOException {
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
        for (PacketChunk chunk : chunks) {
            if (chunk.getTrack() == trackId) {
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

                int compositionOffset = (int)(pts - ptsEstimate);
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
            stbl.add(new SyncSamplesBox(stss.toArray()));

        stbl.add(new ChunkOffsets64Box(stco.toArray()));
        stbl.add(new SampleToChunkBox(new SampleToChunkEntry[] { new SampleToChunkEntry(1, 1, 1) }));
        stbl.add(new SampleSizesBox(stsz.toArray()));
        stbl.add(new TimeToSampleBox(stts.toArray(new TimeToSampleEntry[stts.size()])));
        compositionOffsets(compositionOffsets, stbl);
    }

    private static void compositionOffsets(List<Entry> compositionOffsets, NodeBox stbl) {
        if (compositionOffsets.size() > 0) {
            int min = FramesMP4MuxerTrack.minOffset(compositionOffsets);
            for (Entry entry : compositionOffsets) {
                entry.offset -= min;
            }
            stbl.add(new CompositionOffsetsBox(compositionOffsets.toArray(new Entry[compositionOffsets.size()])));
        }
    }

    private static void populateStblPCM(NodeBox stbl, PacketChunk[] chunks, int trackId, SampleEntry se)
            throws IOException {
        AudioSampleEntry ase = (AudioSampleEntry) se;
        int frameSize = ase.calcFrameSize();

        LongArrayList stco = new LongArrayList(250 << 10);
        List<SampleToChunkEntry> stsc = new ArrayList<SampleToChunkEntry>();
        int stscCount = -1, stscFirstChunk = -1, totalFrames = 0;

        for (int chunkNo = 0, stscCurChunk = 1; chunkNo < chunks.length; chunkNo++) {
            PacketChunk chunk = chunks[chunkNo];

            if (chunk.getTrack() == trackId) {
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

        stbl.add(new ChunkOffsets64Box(stco.toArray()));
        stbl.add(new SampleToChunkBox(stsc.toArray(new SampleToChunkEntry[stsc.size()])));
        stbl.add(new SampleSizesBox(ase.calcFrameSize(), totalFrames));
        stbl.add(new TimeToSampleBox(new TimeToSampleEntry[] { new TimeToSampleEntry(totalFrames, 1) }));
    }

    private static int getPCMTs(AudioSampleEntry se, PacketChunk[] chunks, int trackId) throws IOException {
        for (PacketChunk chunk : chunks) {
            if (chunk.getTrack() == trackId) {
                return (int) Math.round(chunk.getDataLen()
                        / (se.calcFrameSize() * chunk.getPacket().getDuration()));
            }
        }
        throw new RuntimeException("Crap");
    }

    private static void mediaHeader(MediaInfoBox minf, TrackType type) {
        switch (type) {
        case VIDEO:
            VideoMediaHeaderBox vmhd = new VideoMediaHeaderBox(0, 0, 0, 0);
            vmhd.setFlags(1);
            minf.add(vmhd);
            break;
        case SOUND:
            SoundMediaHeaderBox smhd = new SoundMediaHeaderBox();
            smhd.setFlags(1);
            minf.add(smhd);
            break;
        case TIMECODE:
            NodeBox gmhd = new NodeBox(new Header("gmhd"));
            gmhd.add(new GenericMediaInfoBox());
            NodeBox tmcd = new NodeBox(new Header("tmcd"));
            gmhd.add(tmcd);
            tmcd.add(new TimecodeMediaInfoBox((short) 0, (short) 0, (short) 12, new short[] { 0, 0, 0 }, new short[] {
                    0xff, 0xff, 0xff }, "Lucida Grande"));
            minf.add(gmhd);
            break;
        default:
            throw new IllegalStateException("Handler " + type.getHandler() + " not supported");
        }
    }

    private static void addDref(NodeBox minf) {
        DataInfoBox dinf = new DataInfoBox();
        minf.add(dinf);
        DataRefBox dref = new DataRefBox();
        dinf.add(dref);
        dref.add(new LeafBox(new Header("alis", 0), ByteBuffer.wrap(new byte[] { 0, 0, 0, 1 })));
    }

    private static MovieHeaderBox movieHeader(NodeBox movie, int nTracks, long duration, int timescale) {

        return new MovieHeaderBox(timescale, duration, 1.0f, 1.0f, new Date().getTime(), new Date().getTime(),
                new int[] { 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000 }, nTracks + 1);
    }
}