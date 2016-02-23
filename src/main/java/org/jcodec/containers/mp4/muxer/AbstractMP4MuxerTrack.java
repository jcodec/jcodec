package org.jcodec.containers.mp4.muxer;

import static org.jcodec.containers.mp4.TrackType.SOUND;
import static org.jcodec.containers.mp4.TrackType.TIMECODE;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.api.UnhandledStateException;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ClearApertureBox;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.EditListBox;
import org.jcodec.containers.mp4.boxes.EncodedPixelBox;
import org.jcodec.containers.mp4.boxes.GenericMediaInfoBox;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NameBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.ProductionApertureBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.SoundMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.TimecodeMediaInfoBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoMediaHeaderBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class AbstractMP4MuxerTrack {
    protected int trackId;
    protected TrackType type;
    protected int timescale;

    protected Rational tgtChunkDuration;
    protected Unit tgtChunkDurationUnit;

    protected long chunkDuration;
    protected List<ByteBuffer> curChunk;

    protected List<SampleToChunkEntry> samplesInChunks;
    protected int samplesInLastChunk = -1;
    protected int chunkNo = 0;

    protected boolean finished;

    protected List<SampleEntry> sampleEntries = new ArrayList<SampleEntry>();
    protected List<Edit> edits;
    private String name;

    public AbstractMP4MuxerTrack(int trackId, TrackType type, int timescale) {
        this.curChunk = new ArrayList<ByteBuffer>();
        this.samplesInChunks = new ArrayList<SampleToChunkEntry>();

        this.trackId = trackId;
        this.type = type;
        this.timescale = timescale;
    }

    public void setTgtChunkDuration(Rational duration, Unit unit) {
        this.tgtChunkDuration = duration;
        this.tgtChunkDurationUnit = unit;
    }

    public abstract long getTrackTotalDuration();

    public int getTimescale() {
        return timescale;
    }

    protected abstract Box finish(MovieHeaderBox mvhd) throws IOException;

    public boolean isVideo() {
        return type == VIDEO;
    }

    public boolean isTimecode() {
        return type == TIMECODE;
    }

    public boolean isAudio() {
        return type == SOUND;
    }

    public Size getDisplayDimensions() {
        int width = 0, height = 0;
        if (sampleEntries.get(0) instanceof VideoSampleEntry) {
            VideoSampleEntry vse = (VideoSampleEntry) sampleEntries.get(0);
            PixelAspectExt paspBox = Box.findFirst(vse, PixelAspectExt.class, PixelAspectExt.fourcc());
            Rational pasp = paspBox != null ? paspBox.getRational() : new Rational(1, 1);
            width = (int) (pasp.getNum() * vse.getWidth()) / pasp.getDen();
            height = (int) vse.getHeight();
        }
        return new Size(width, height);
    }

    public void tapt(TrakBox trak) {
        Size dd = getDisplayDimensions();
        if (type == VIDEO) {
            NodeBox tapt = new NodeBox(new Header("tapt"));
            tapt.add(new ClearApertureBox(dd.getWidth(), dd.getHeight()));
            tapt.add(new ProductionApertureBox(dd.getWidth(), dd.getHeight()));
            tapt.add(new EncodedPixelBox(dd.getWidth(), dd.getHeight()));
            trak.add(tapt);
        }
    }

    public void addSampleEntry(SampleEntry se) {
        if (finished)
            throw new IllegalStateException("The muxer track has finished muxing");
        sampleEntries.add(se);
    }

    public List<SampleEntry> getEntries() {
        return sampleEntries;
    }

    public void setEdits(List<Edit> edits) {
        this.edits = edits;
    }

    protected void putEdits(TrakBox trak) {
        if (edits != null) {
            NodeBox edts = new NodeBox(new Header("edts"));
            edts.add(new EditListBox(edits));
            trak.add(edts);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    protected void putName(TrakBox trak) {
        if (name != null) {
            NodeBox udta = new NodeBox(new Header("udta"));
            udta.add(new NameBox(name));
            trak.add(udta);
        }
    }
    
    protected void mediaHeader(MediaInfoBox minf, TrackType type) {
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
            throw new UnhandledStateException("Handler " + type.getHandler() + " not supported");
        }
    }

    protected void addDref(NodeBox minf) {
        DataInfoBox dinf = new DataInfoBox();
        minf.add(dinf);
        DataRefBox dref = new DataRefBox();
        dinf.add(dref);
        dref.add(new LeafBox(new Header("alis", 0), ByteBuffer.wrap(new byte[] { 0, 0, 0, 1 })));
    }
}