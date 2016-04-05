package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;

import js.lang.IllegalArgumentException;
import js.util.List;
import js.util.ListIterator;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class TrakBox extends NodeBox {

    public static String fourcc() {
        return "trak";
    }

    public static TrakBox createTrakBox() {
        return new TrakBox(new Header(fourcc()));
    }

    public TrakBox(Header atom) {
        super(atom);
    }

    public void setDataRef(String url) {
        MediaInfoBox minf = getMdia().getMinf();
        DataInfoBox dinf = minf.getDinf();
        if (dinf == null) {
            dinf = DataInfoBox.createDataInfoBox();
            minf.add(dinf);
        }
        DataRefBox dref = dinf.getDref();
        UrlBox urlBox = UrlBox.createUrlBox(url);
        if (dref == null) {
            dref = DataRefBox.createDataRefBox();
            dinf.add(dref);
            dref.add(urlBox);
        } else {
            ListIterator<Box> lit = dref.boxes.listIterator();
            while (lit.hasNext()) {
                FullBox box = (FullBox) lit.next();
                if ((box.getFlags() & 0x1) != 0)
                    lit.set(urlBox);
            }
        }
    }

    public MediaBox getMdia() {
        return NodeBox.findFirst(this, MediaBox.class, "mdia");
    }

    public TrackHeaderBox getTrackHeader() {
        return NodeBox.findFirst(this, TrackHeaderBox.class, "tkhd");
    }

    public List<Edit> getEdits() {
        EditListBox elst = NodeBox.findFirstPath(this, EditListBox.class, Box.path("edts.elst"));
        if (elst == null)
            return null;
        return elst.getEdits();
    }

    public void setEdits(List<Edit> edits) {
        NodeBox edts = NodeBox.findFirst(this, NodeBox.class, "edts");
        if (edts == null) {
            edts = new NodeBox(new Header("edts"));
            this.add(edts);
        }
        edts.removeChildren("elst");

        edts.add(EditListBox.createEditListBox(edits));
        getTrackHeader().setDuration(getEditedDuration(this));
    }

    public boolean isVideo() {
        return "vide".equals(getHandlerType());
    }

    public boolean isTimecode() {
        return "tmcd".equals(getHandlerType());
    }

    public String getHandlerType() {
        HandlerBox handlerBox = NodeBox.findFirstPath(this, HandlerBox.class, Box.path("mdia.hdlr"));
        if (handlerBox == null)
            return null;
        String type = handlerBox.getComponentSubType();
        return type;
    }

    public boolean isAudio() {
        return "soun".equals(getHandlerType());
    }

    /**
     * Gets 'media timescale' of this track. This is the timescale used to
     * represent the durations of samples inside mdia/minf/stbl/stts box.
     * 
     * @return 'media timescale' of the track.
     */
    public int getTimescale() {
        return NodeBox.findFirstPath(this, MediaHeaderBox.class, Box.path("mdia.mdhd")).getTimescale();
    }

    /**
     * Sets the 'media timescale' of this track. This is the time timescale used
     * to represent sample durations.
     * 
     * @param timescale
     *            A new 'media timescale' of this track.
     */
    public void setTimescale(int timescale) {
        NodeBox.findFirstPath(this, MediaHeaderBox.class, Box.path("mdia.mdhd")).setTimescale(timescale);
    }

    public long rescale(long tv, long ts) {
        return (tv * getTimescale()) / ts;
    }

    public void setDuration(long duration) {
        getTrackHeader().setDuration(duration);
    }

    public long getDuration() {
        return getTrackHeader().getDuration();
    }

    public long getMediaDuration() {
        return NodeBox.findFirstPath(this, MediaHeaderBox.class, Box.path("mdia.mdhd")).getDuration();
    }

    public boolean isPureRef() {
        MediaInfoBox minf = getMdia().getMinf();
        DataInfoBox dinf = minf.getDinf();
        if (dinf == null) {
            return false;
        }
        DataRefBox dref = dinf.getDref();
        if (dref == null)
            return false;

        for (Box box : dref.boxes) {
            if ((((FullBox) box).getFlags() & 0x1) != 0)
                return false;
        }
        return true;
    }

    public boolean hasDataRef() {
        DataInfoBox dinf = getMdia().getMinf().getDinf();
        if (dinf == null) {
            return false;
        }
        DataRefBox dref = dinf.getDref();
        if (dref == null)
            return false;

        boolean result = false;
        for (Box box : dref.boxes) {
            result |= (((FullBox) box).getFlags() & 0x1) != 0x1;
        }
        return result;
    }

    public Rational getPAR() {
        PixelAspectExt pasp = NodeBox.findFirstPath(this, PixelAspectExt.class, new String[] { "mdia", "minf", "stbl", "stsd", null, "pasp" });
        return pasp == null ? new Rational(1, 1) : pasp.getRational();
    }

    public void setPAR(Rational par) {
        SampleEntry[] sampleEntries = getSampleEntries();
        for (int i = 0; i < sampleEntries.length; i++) {
            SampleEntry sampleEntry = sampleEntries[i];
            sampleEntry.removeChildren("pasp");
            sampleEntry.add(PixelAspectExt.createPixelAspectExt(par));
        }
    }

    public SampleEntry[] getSampleEntries() {
        return NodeBox.findAllPath(this, SampleEntry.class, new String[]{"mdia", "minf", "stbl", "stsd", null});
    }

    public void setClipRect(short x, short y, short width, short height) {
        NodeBox clip = NodeBox.findFirst(this, NodeBox.class, "clip");
        if (clip == null) {
            clip = new NodeBox(new Header("clip"));
            add(clip);
        }
        clip.replace("crgn", ClipRegionBox.createClipRegionBox(x, y, width, height));
    }

    public long getSampleCount() {
        return NodeBox.findFirstPath(this, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz")).getCount();
    }

    public void setAperture(Size sar, Size dar) {
        removeChildren("tapt");
        NodeBox tapt = new NodeBox(new Header("tapt"));
        tapt.add(ClearApertureBox.createClearApertureBox(dar.getWidth(), dar.getHeight()));
        tapt.add(ProductionApertureBox.createProductionApertureBox(dar.getWidth(), dar.getHeight()));
        tapt.add(EncodedPixelBox.createEncodedPixelBox(sar.getWidth(), sar.getHeight()));
        add(tapt);
    }

    public void setDimensions(Size dd) {
        getTrackHeader().setWidth((float) dd.getWidth());
        getTrackHeader().setHeight((float) dd.getHeight());
    }

    public int getFrameCount() {
        SampleSizesBox stsz = NodeBox.findFirstPath(this, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz"));
        return stsz.getDefaultSize() != 0 ? stsz.getCount() : stsz.getSizes().length;
    }

    public String getName() {
        NameBox nb = NodeBox.findFirstPath(this, NameBox.class, Box.path("udta.name"));
        return nb == null ? null : nb.getName();
    }

    public void fixMediaTimescale(int ts) {
        MediaHeaderBox mdhd = NodeBox.findFirstPath(this, MediaHeaderBox.class, Box.path("mdia.mdhd"));
        int oldTs = mdhd.getTimescale();

        mdhd.setTimescale(ts);
        mdhd.setDuration((ts * mdhd.getDuration()) / oldTs);
        List<Edit> edits = getEdits();
        if (edits != null) {
            for (Edit edit : edits) {
                edit.setMediaTime((ts * edit.getMediaTime()) / oldTs);
            }
        }
        TimeToSampleBox tts = NodeBox.findFirstPath(this, TimeToSampleBox.class, Box.path("mdia.minf.stbl.stts"));
        TimeToSampleEntry[] entries = tts.getEntries();
        for (int i = 0; i < entries.length; i++) {
            TimeToSampleEntry tte = entries[i];
            tte.setSampleDuration((ts * tte.getSampleDuration()) / oldTs);
        }
    }

    public void setName(String string) {
        NodeBox udta = NodeBox.findFirst(this, NodeBox.class, "udta");
        if (udta == null) {
            udta = new NodeBox(new Header("udta"));
            this.add(udta);
        }
        udta.removeChildren("name");
        udta.add(NameBox.createNameBox(string));
    }

    /**
     * Retrieves coded size of this video track.
     * 
     * Note: May be different from video display dimension.
     * 
     * @return
     */
    public Size getCodedSize() {
        SampleEntry se = getSampleEntries()[0];
        if (!(se instanceof VideoSampleEntry))
            throw new IllegalArgumentException("Not a video track");
        VideoSampleEntry vse = (VideoSampleEntry) se;

        return new Size(vse.getWidth(), vse.getHeight());
    }

    protected void getModelFields(List<String> model) {

    }

    public TimeToSampleBox getStts() {
        return NodeBox.findFirstPath(this, TimeToSampleBox.class, Box.path("mdia.minf.stbl.stts"));
    }

    public ChunkOffsetsBox getStco() {
        return NodeBox.findFirstPath(this, ChunkOffsetsBox.class, Box.path("mdia.minf.stbl.stco" ));
    }

    public ChunkOffsets64Box getCo64() {
        return NodeBox.findFirstPath(this, ChunkOffsets64Box.class, Box.path("mdia.minf.stbl.co64" ));
    }

    public SampleSizesBox getStsz() {
        return NodeBox.findFirstPath(this, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz" ));
    }

    public SampleToChunkBox getStsc() {
        return NodeBox.findFirstPath(this, SampleToChunkBox.class, Box.path("mdia.minf.stbl.stsc" ));
    }

    public SampleDescriptionBox getStsd() {
        return NodeBox.findFirstPath(this, SampleDescriptionBox.class, Box.path("mdia.minf.stbl.stsd" ));
    }

    public SyncSamplesBox getStss() {
        return NodeBox.findFirstPath(this, SyncSamplesBox.class, Box.path("mdia.minf.stbl.stss" ));
    }

    public CompositionOffsetsBox getCtts() {
        return NodeBox.findFirstPath(this, CompositionOffsetsBox.class, Box.path("mdia.minf.stbl.ctts" ));
    }
    
    public static TrackType getTrackType(TrakBox trak) {
        HandlerBox handler = NodeBox.findFirstPath(trak, HandlerBox.class, Box.path("mdia.hdlr"));
        return TrackType.fromHandler(handler.getComponentSubType());
    }

    /**
     * Calculates track duration considering edits
     * 
     * @param track
     * @return
     */
    public static long getEditedDuration(TrakBox track) {
        List<Edit> edits = track.getEdits();
        if (edits == null)
            return track.getDuration();

        long duration = 0;
        for (Edit edit : edits) {
            duration += edit.getDuration();
        }
        return duration;
    }
}