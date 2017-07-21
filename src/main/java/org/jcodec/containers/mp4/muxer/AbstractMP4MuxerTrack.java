package org.jcodec.containers.mp4.muxer;
import static org.jcodec.common.Preconditions.checkState;
import static org.jcodec.containers.mp4.MP4TrackType.SOUND;
import static org.jcodec.containers.mp4.MP4TrackType.TIMECODE;
import static org.jcodec.containers.mp4.MP4TrackType.VIDEO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.api.UnhandledStateException;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;
import org.jcodec.containers.mp4.boxes.ClearApertureBox;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.EditListBox;
import org.jcodec.containers.mp4.boxes.EncodedPixelBox;
import org.jcodec.containers.mp4.boxes.GenericMediaInfoBox;
import org.jcodec.containers.mp4.boxes.Header;
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
public abstract class AbstractMP4MuxerTrack implements MuxerTrack {
    protected static final int NO_TIMESCALE_SET = -1;
    
    protected int trackId;
    protected MP4TrackType type;
    protected int _timescale = NO_TIMESCALE_SET;

    protected Rational tgtChunkDuration;
    protected Unit tgtChunkDurationUnit;

    protected long chunkDuration;
    protected List<ByteBuffer> curChunk;

    protected List<SampleToChunkEntry> samplesInChunks;
    protected int samplesInLastChunk = -1;
    protected int chunkNo = 0;

    protected boolean finished;

    protected List<SampleEntry> sampleEntries;
    protected List<Edit> edits;
    private String name;

    protected SeekableByteChannel out;

    public AbstractMP4MuxerTrack(int trackId, MP4TrackType type) {
        this.curChunk = new ArrayList<ByteBuffer>();
        this.samplesInChunks = new ArrayList<SampleToChunkEntry>();
        this.sampleEntries = new ArrayList<SampleEntry>();

        this.trackId = trackId;
        this.type = type;
    }
    
    AbstractMP4MuxerTrack setOut(SeekableByteChannel out) {
        this.out = out;
        return this;
    }

    public void setTgtChunkDuration(Rational duration, Unit unit) {
        this.tgtChunkDuration = duration;
        this.tgtChunkDurationUnit = unit;
    }

    public abstract long getTrackTotalDuration();

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
    
    public MP4TrackType getType() {
        return type;
    }
    
    public int getTrackId() {
        return trackId;
    }

    public Size getDisplayDimensions() {
        int width = 0, height = 0;
        if (sampleEntries != null && !sampleEntries.isEmpty() && sampleEntries.get(0) instanceof VideoSampleEntry) {
            VideoSampleEntry vse = (VideoSampleEntry) sampleEntries.get(0);
            PixelAspectExt paspBox = NodeBox.findFirst(vse, PixelAspectExt.class, PixelAspectExt.fourcc());
            Rational pasp = paspBox != null ? paspBox.getRational() : new Rational(1, 1);
            width = pasp.getNum() * vse.getWidth() / pasp.getDen();
            height = vse.getHeight();
        }
        return new Size(width, height);
    }

    public void tapt(TrakBox trak) {
        Size dd = getDisplayDimensions();
        if (type == VIDEO) {
            NodeBox tapt = new NodeBox(new Header("tapt"));
            tapt.add(ClearApertureBox.createClearApertureBox(dd.getWidth(), dd.getHeight()));
            tapt.add(ProductionApertureBox.createProductionApertureBox(dd.getWidth(), dd.getHeight()));
            tapt.add(EncodedPixelBox.createEncodedPixelBox(dd.getWidth(), dd.getHeight()));
            trak.add(tapt);
        }
    }

    public AbstractMP4MuxerTrack addSampleEntry(SampleEntry se) {
        checkState(!finished, "The muxer track has finished muxing");
        sampleEntries.add(se);
        return this;
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
            edts.add(EditListBox.createEditListBox(edits));
            trak.add(edts);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    protected void putName(TrakBox trak) {
        if (name != null) {
            NodeBox udta = new NodeBox(new Header("udta"));
            udta.add(NameBox.createNameBox(name));
            trak.add(udta);
        }
    }
    
    protected void mediaHeader(MediaInfoBox minf, MP4TrackType type) {
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
        } else if(MP4TrackType.DATA == type) {
            //do nothing
        } else {
            throw new UnhandledStateException("Handler " + type.getHandler() + " not supported");
        }
    }

    protected void addDref(NodeBox minf) {
        DataInfoBox dinf = DataInfoBox.createDataInfoBox();
        minf.add(dinf);
        DataRefBox dref = DataRefBox.createDataRefBox();
        dinf.add(dref);
        dref.add(LeafBox.createLeafBox(Header.createHeader("alis", 0), ByteBuffer.wrap(new byte[] { 0, 0, 0, 1 })));
    }

    protected int getTimescale() {
        return _timescale;
    }
}