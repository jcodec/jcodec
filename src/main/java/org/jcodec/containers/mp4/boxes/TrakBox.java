package org.jcodec.containers.mp4.boxes;

import static org.jcodec.containers.mp4.QTTimeUtil.getEditedDuration;

import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections15.Predicate;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
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

    public TrakBox(Header atom) {
        super(atom);
    }

    public TrakBox() {
        super(new Header(fourcc()));
    }

    public void setDataRef(String url) {
        MediaInfoBox minf = getMdia().getMinf();
        DataInfoBox dinf = minf.getDinf();
        if (dinf == null) {
            dinf = new DataInfoBox();
            minf.add(dinf);
        }
        DataRefBox dref = dinf.getDref();
        UrlBox urlBox = new UrlBox(url);
        if (dref == null) {
            dref = new DataRefBox();
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
        return findFirst(this, MediaBox.class, "mdia");
    }

    public TrackHeaderBox getTrackHeader() {
        return findFirst(this, TrackHeaderBox.class, "tkhd");
    }

    public List<Edit> getEdits() {
        EditListBox elst = findFirst(this, EditListBox.class, "edts", "elst");
        if (elst == null)
            return null;
        return elst.getEdits();
    }

    public void setEdits(List<Edit> edits) {
        NodeBox edts = findFirst(this, NodeBox.class, "edts");
        if (edts == null) {
            edts = new NodeBox(new Header("edts"));
            this.add(edts);
        }
        edts.filter(Box.not("elst"));

        edts.add(new EditListBox(edits));
        getTrackHeader().setDuration(getEditedDuration(this));
    }

    

    public boolean isVideo() {
        return "vide".equals(getHandlerType());
    }

    public boolean isTimecode() {
        return "tmcd".equals(getHandlerType());
    }

    public String getHandlerType() {
        HandlerBox handlerBox = findFirst(this, HandlerBox.class, "mdia", "hdlr");
        if (handlerBox == null)
            return null;
        String type = handlerBox.getComponentSubType();
        return type;
    }

    public boolean isAudio() {
        return "soun".equals(getHandlerType());
    }

    public int getTimescale() {
        return findFirst(this, MediaHeaderBox.class, "mdia", "mdhd").getTimescale();
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

    public static Predicate<TrakBox> videoTrack() {
        return new Predicate<TrakBox>() {
            public boolean evaluate(TrakBox box) {
                return box.isVideo();
            }
        };
    }

    public static Predicate<Box> audioTrack() {
        return new Predicate<Box>() {
            public boolean evaluate(Box box) {
                if (!(box instanceof TrakBox))
                    return false;
                if (((TrakBox) box).isAudio())
                    return true;
                return false;
            }
        };
    }

    public long getMediaDuration() {
        return findFirst(this, MediaHeaderBox.class, "mdia", "mdhd").getDuration();
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
        PixelAspectExt pasp = NodeBox.findFirst(this, PixelAspectExt.class, "mdia", "minf", "stbl", "stsd", null,
                "pasp");
        return pasp == null ? new Rational(1, 1) : pasp.getRational();
    }

    public void setPAR(Rational par) {
        for (SampleEntry sampleEntry : getSampleEntries()) {
            sampleEntry.filter(not("pasp"));
            sampleEntry.add(new PixelAspectExt(par));
        }
    }

    public SampleEntry[] getSampleEntries() {
        return NodeBox.findAll(this, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null);
    }

    public void setClipRect(short x, short y, short width, short height) {
        NodeBox clip = NodeBox.findFirst(this, NodeBox.class, "clip");
        if (clip == null) {
            clip = new NodeBox(new Header("clip"));
            add(clip);
        }
        clip.replace("crgn", new ClipRegionBox(x, y, width, height));
    }

    public long getSampleCount() {
        return NodeBox.findFirst(this, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz").getCount();
    }

    public void setAperture(Size sar, Size dar) {
        filter(Box.not("tapt"));
        NodeBox tapt = new NodeBox(new Header("tapt"));
        tapt.add(new ClearApertureBox(dar.getWidth(), dar.getHeight()));
        tapt.add(new ProductionApertureBox(dar.getWidth(), dar.getHeight()));
        tapt.add(new EncodedPixelBox(sar.getWidth(), sar.getHeight()));
        add(tapt);
    }

    public void setDimensions(Size dd) {
        getTrackHeader().setWidth((float) dd.getWidth());
        getTrackHeader().setHeight((float) dd.getHeight());
    }

    public int getFrameCount() {
        SampleSizesBox stsz = findFirst(this, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
        return stsz.getDefaultSize() != 0 ? stsz.getCount() : stsz.getSizes().length;
    }

    public String getName() {
        NameBox nb = Box.findFirst(this, NameBox.class, "udta", "name");
        return nb == null ? null : nb.getName();
    }

    public void fixMediaTimescale(int ts) {
        MediaHeaderBox mdhd = Box.findFirst(this, MediaHeaderBox.class, "mdia", "mdhd");
        int oldTs = mdhd.getTimescale();

        mdhd.setTimescale(ts);
        mdhd.setDuration((ts * mdhd.getDuration()) / oldTs);
        List<Edit> edits = getEdits();
        if (edits != null) {
            for (Edit edit : edits) {
                edit.setMediaTime((ts * edit.getMediaTime()) / oldTs);
            }
        }
        TimeToSampleBox tts = Box.findFirst(this, TimeToSampleBox.class, "mdia", "minf", "stbl", "stts");
        TimeToSampleEntry[] entries = tts.getEntries();
        for (TimeToSampleEntry tte : entries) {
            tte.setSampleDuration((ts * tte.getSampleDuration()) / oldTs);
        }
    }

    public void setName(String string) {
        NodeBox udta = findFirst(this, NodeBox.class, "udta");
        if (udta == null) {
            udta = new NodeBox(new Header("udta"));
            this.add(udta);
        }
        udta.filter(Box.not("name"));
        udta.add(new NameBox(string));
    }
}