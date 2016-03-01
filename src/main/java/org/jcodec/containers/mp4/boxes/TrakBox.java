package org.jcodec.containers.mp4.boxes;

import static org.jcodec.containers.mp4.QTTimeUtil.getEditedDuration;

import java.util.List;
import java.util.ListIterator;

import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;

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
        return BoxUtil.findFirst(this, MediaBox.class, "mdia");
    }

    public TrackHeaderBox getTrackHeader() {
        return BoxUtil.findFirst(this, TrackHeaderBox.class, "tkhd");
    }

    public List<Edit> getEdits() {
        EditListBox elst = BoxUtil.findFirstPath(this, EditListBox.class, BoxUtil.path("edts.elst"));
        if (elst == null)
            return null;
        return elst.getEdits();
    }

    public void setEdits(List<Edit> edits) {
        NodeBox edts = BoxUtil.findFirst(this, NodeBox.class, "edts");
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
        HandlerBox handlerBox = BoxUtil.findFirstPath(this, HandlerBox.class, BoxUtil.path("mdia.hdlr"));
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
        return BoxUtil.findFirstPath(this, MediaHeaderBox.class, BoxUtil.path("mdia.mdhd")).getTimescale();
    }

    /**
     * Sets the 'media timescale' of this track. This is the time timescale used
     * to represent sample durations.
     * 
     * @param timescale
     *            A new 'media timescale' of this track.
     */
    public void setTimescale(int timescale) {
        BoxUtil.findFirstPath(this, MediaHeaderBox.class, BoxUtil.path("mdia.mdhd")).setTimescale(timescale);
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
        return BoxUtil.findFirstPath(this, MediaHeaderBox.class, BoxUtil.path("mdia.mdhd")).getDuration();
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
        PixelAspectExt pasp = BoxUtil.findFirstPath(this, PixelAspectExt.class, new String[] { "mdia", "minf", "stbl", "stsd", null, "pasp" });
        return pasp == null ? new Rational(1, 1) : pasp.getRational();
    }

    public void setPAR(Rational par) {
        for (SampleEntry sampleEntry : getSampleEntries()) {
            sampleEntry.removeChildren("pasp");
            sampleEntry.add(PixelAspectExt.createPixelAspectExt(par));
        }
    }

    public SampleEntry[] getSampleEntries() {
        return BoxUtil.findAllPath(this, SampleEntry.class, new String[]{"mdia", "minf", "stbl", "stsd", null});
    }

    public void setClipRect(short x, short y, short width, short height) {
        NodeBox clip = BoxUtil.findFirst(this, NodeBox.class, "clip");
        if (clip == null) {
            clip = new NodeBox(new Header("clip"));
            add(clip);
        }
        clip.replace("crgn", ClipRegionBox.createClipRegionBox(x, y, width, height));
    }

    public long getSampleCount() {
        return BoxUtil.findFirstPath(this, SampleSizesBox.class, BoxUtil.path("mdia.minf.stbl.stsz")).getCount();
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
        SampleSizesBox stsz = BoxUtil.findFirstPath(this, SampleSizesBox.class, BoxUtil.path("mdia.minf.stbl.stsz"));
        return stsz.getDefaultSize() != 0 ? stsz.getCount() : stsz.getSizes().length;
    }

    public String getName() {
        NameBox nb = BoxUtil.findFirstPath(this, NameBox.class, BoxUtil.path("udta.name"));
        return nb == null ? null : nb.getName();
    }

    public void fixMediaTimescale(int ts) {
        MediaHeaderBox mdhd = BoxUtil.findFirstPath(this, MediaHeaderBox.class, BoxUtil.path("mdia.mdhd"));
        int oldTs = mdhd.getTimescale();

        mdhd.setTimescale(ts);
        mdhd.setDuration((ts * mdhd.getDuration()) / oldTs);
        List<Edit> edits = getEdits();
        if (edits != null) {
            for (Edit edit : edits) {
                edit.setMediaTime((ts * edit.getMediaTime()) / oldTs);
            }
        }
        TimeToSampleBox tts = BoxUtil.findFirstPath(this, TimeToSampleBox.class, BoxUtil.path("mdia.minf.stbl.stts"));
        TimeToSampleEntry[] entries = tts.getEntries();
        for (TimeToSampleEntry tte : entries) {
            tte.setSampleDuration((ts * tte.getSampleDuration()) / oldTs);
        }
    }

    public void setName(String string) {
        NodeBox udta = BoxUtil.findFirst(this, NodeBox.class, "udta");
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
        return BoxUtil.findFirstPath(this, TimeToSampleBox.class, BoxUtil.path("mdia.minf.stbl.stts"));
    }

    public ChunkOffsetsBox getStco() {
        return BoxUtil.findFirstPath(this, ChunkOffsetsBox.class, BoxUtil.path("mdia.minf.stbl.stco" ));
    }

    public ChunkOffsets64Box getCo64() {
        return BoxUtil.findFirstPath(this, ChunkOffsets64Box.class, BoxUtil.path("mdia.minf.stbl.co64" ));
    }

    public SampleSizesBox getStsz() {
        return BoxUtil.findFirstPath(this, SampleSizesBox.class, BoxUtil.path("mdia.minf.stbl.stsz" ));
    }

    public SampleToChunkBox getStsc() {
        return BoxUtil.findFirstPath(this, SampleToChunkBox.class, BoxUtil.path("mdia.minf.stbl.stsc" ));
    }

    public SampleDescriptionBox getStsd() {
        return BoxUtil.findFirstPath(this, SampleDescriptionBox.class, BoxUtil.path("mdia.minf.stbl.stsd" ));
    }

    public SyncSamplesBox getStss() {
        return BoxUtil.findFirstPath(this, SyncSamplesBox.class, BoxUtil.path("mdia.minf.stbl.stss" ));
    }

    public CompositionOffsetsBox getCtts() {
        return BoxUtil.findFirstPath(this, CompositionOffsetsBox.class, BoxUtil.path("mdia.minf.stbl.ctts" ));
    }
}