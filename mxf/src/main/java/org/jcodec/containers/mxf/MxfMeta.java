package org.jcodec.containers.mxf;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.junit.Assert.assertFalse;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.Fraction;
import org.jcodec.common.model.Rational;

import com.vg.io.SeekableFileInputStream;
import com.vg.io.SeekableInputStream;
import com.vg.mxf.CDCIEssenceDescriptor;
import com.vg.mxf.GenericPictureEssenceDescriptor;
import com.vg.mxf.MxfStructure;
import com.vg.mxf.RGBAEssenceDescriptor;
import com.vg.mxf.TimecodeComponent;
import com.vg.mxf.TimelineTrack;

public class MxfMeta {
    final int dropFrame;

    public boolean isDropFrame() {
        return dropFrame != 0;
    }

    public final Rational editRate;
    public final int roundedTimecodeBase;
    public final int startTimecode;

    /**
     * Images may be scanned progressively or in one of several interlaced
     * methods. Frame layout property is as follows:
     * <ul>
     * <li>FULL_FRAME (0) Ð a progressive lattice from top to bottom, stored in
     * progressive line order 1,2,3,4,5,6... Example: Ò480P59.94Ó. The duration
     * of a sampled rectangle is a frame.</li>
     * <li>SEPARATE_FIELDS (1) Ð an interlaced lattice divided into two fields,
     * stored as two fields 1,3,5,... and 2,4,6... Field 1 scans alternate lines
     * from top to bottom, field 2 scans the intervening lines. The second field
     * is scanned at a later time than the first field (one field later). Note
     * that different signal standards may be different topness (see E.2.16) and
     * dominance (see E.2.25). Examples: NTSC, ANSI/SMPTE 125M. The duration of
     * a sampled rectangle is a field.</li>
     * <li>SINGLE_FIELD (2) Ð an interlaced lattice as for SEPARATE_FIELDS
     * above, except that only one field is scanned and retained in the stored
     * data, as 1,3,5,... or 2,4,6,... or (1+2),(3+4),(5+6),... For display, the
     * second field is derived by line replication or interpolation. There are
     * no examples of SINGLE_FIELD in broadcast use; however, this type of sub-
     * sampling is often used as a simple compression for index frames. The
     * duration of a sampled rectangle is a frame.</li>
     * <li>MIXED_FIELDS (3) Ð an interlaced lattice as for SEPARATE_FIELDS
     * above, stored as a single matrix of interleaved lines 1,2,3,4,5,6,... It
     * is not common to use MIXED_FIELDS in broadcast; however, intermediate
     * in-memory data structures sometimes use this format. The duration of a
     * sampled rectangle is a frame.</li>
     * <li>SEGMENTED_FRAME (4) - an interlaced lattice divided into two fields.
     * Field 1 scans alternate lines from top to bottom, field 2 scans the
     * intervening lines. The lines are stored as two fields 1,3,5,...
     * 2,4,6,...The two fields are taken from a single scan of the incoming
     * image Ð i.e., they are coincident in time, except for the effects of
     * shutter angle. Example: Ò1080P24 SFÓ. The duration of a sampled rectangle
     * is a field.</li>
     * </ul>
     * <h3>NOTES</h3>
     * <ol>
     * <li>Field by field compression of an interlaced signal results in a frame
     * layout of SEPARATE_FIELDS. This includes DV compression Ð even though the
     * compressed data stream may be grouped into whole frames or fields, and
     * even though in some cases DV uses adaptive field/frame compression, and
     * always requires both fields of compressed data in order to successfully
     * decompress. It also includes the adaptive field/frame compression mode of
     * MPEG-2.</li>
     * <li>Together with the sample rate property, this property provides the
     * information from which SMPTE 352M byte 2 may be derived.</li>
     * <ol>
     * 
     * 
     */
    final int frameLayout;

    public boolean isInterlaced() {
        return frameLayout != 0;
    }

    /**
     * The number of the field which is considered temporally to come first.
     * (see G.2.19) [RP 210 Specifies whether the first frame of picture is
     * field 1 or field 2]
     */
    final int fieldDominance;
    public final Rational aspectRatio;
    private final Dimension stored;
    private Rectangle display;
    private int frameCount;

    public MxfMeta(int timebase, int startTimecode, Rational editRate, int dropFrame, int frameLayout,
            int fieldDominance, Rational aspectRatio, Dimension stored) {
        this.roundedTimecodeBase = timebase;
        this.startTimecode = startTimecode;
        this.editRate = editRate;
        this.dropFrame = dropFrame;
        this.frameLayout = frameLayout;
        this.fieldDominance = fieldDominance;
        this.aspectRatio = aspectRatio;
        this.stored = stored;
    }

    public static MxfMeta parseMxfFile(File mxf) throws IOException, InterruptedException {
        SeekableInputStream in = new SeekableFileInputStream(mxf);
        try {
            MxfMeta mxfMeta = parseMxfFile(in);
            return mxfMeta;
        } finally {
            in.close();
        }
    }

    public static enum PictureEssenceType {
        RGBA, CDCI;
    }

    PictureEssenceType pictureEssenceType;

    public static MxfMeta parseMxfFile(SeekableInputStream in) throws IOException {
        MxfStructure structure = MxfStructure.readStructure(in);
        MxfMeta mxfMeta = fromMxfStructure(structure);
        return mxfMeta;
    }

    public static MxfMeta fromMxfStructure(MxfStructure structure) throws IOException {
        int start = -1;
        int timebase = -1;
        int dropFrame = -1;
        Rational editRate = Rational.ONE;
        int frameLayout = -1;
        int fieldDominance = 0;
        Rational aspectRatio = null;
        Dimension stored = new Dimension(-1, -1);
        Rectangle display = new Rectangle(-1, -1, -1, -1);

        TimecodeComponent timecode = structure.getTimecodeComponent();
        TimelineTrack timeline = structure.getTimelineTrack();
        GenericPictureEssenceDescriptor pic = structure.getPictureEssenceDescriptor();
        if (timecode != null) {
            start = timecode.getStartTimecode();
            timebase = timecode.getRoundedTimecodeBase();
            dropFrame = timecode.getDropFrame();
        }

        if (timeline != null) {
            Fraction editRate2 = timeline.getEditRate();
            editRate = new Rational(editRate2.getNumerator(), editRate2.getDenominator());
        }

        if (pic != null) {
            frameLayout = pic.getFrameLayout();
            fieldDominance = pic.getFieldDominance();
            Fraction aspectRatio2 = pic.getAspectRatio();
            aspectRatio = new Rational(aspectRatio2.getNumerator(), aspectRatio2.getDenominator());
            stored = pic.getStoredDimension();
            display = pic.getDisplayRectangle();
        }

        if (display.width <= 0 || display.height <= 0) {
            display.width = stored.width;
            display.height = stored.height;
        }

        assertFalse("cant parse StartTimecode from mxf", start == -1);
        assertFalse("cant parse RoundedTimecodeBase from mxf", timebase == -1);
        assertFalse("display width not available", display.width == -1);
        MxfMeta mxfMeta = new MxfMeta(timebase, start, editRate, dropFrame, frameLayout, fieldDominance, aspectRatio,
                stored);
        mxfMeta.display = display;
        mxfMeta.frameCount = structure.getFrameCount();
        if (pic instanceof CDCIEssenceDescriptor) {
            mxfMeta.pictureEssenceType = PictureEssenceType.CDCI;
        } else if (pic instanceof RGBAEssenceDescriptor) {
            mxfMeta.pictureEssenceType = PictureEssenceType.RGBA;
        }

        return mxfMeta;
    }

    public boolean isHD() {
        return stored.width >= 1920;
    }

    @Override
    public String toString() {
        return reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public int getFrameHeight() {
        int h = stored.height;
        if (isInterlaced())
            h = h * 2;
        return h;
    }

    public int getFrameWidth() {
        return stored.width;
    }

    public boolean isSaneHeight() {
        return getFrameHeight() % 8 == 0;
    }

    public Dimension getStoredDimension() {
        Dimension dimension = new Dimension(stored);
        if (isInterlaced()) {
            dimension.height *= 2;
        }
        return dimension;
    }

    public Dimension getDisplayDimension() {
        Dimension dimension = new Dimension(display.width, display.height);
        if (isInterlaced()) {
            dimension.height *= 2;
        }
        return dimension;
    }

    public Rectangle getDisplayRectangle() {
        Rectangle r = new Rectangle(display);
        if (isInterlaced()) {
            r.y *= 2;
            r.height *= 2;
        }
        return r;
    }

//    public TapeTimecode getTapeTimecode() {
//        return new TapeTimecode(this.isDropFrame(), this.startTimecode, this.roundedTimecodeBase);
//    }

    public int getDuration() {
        return 0;
    }

    public long getDurationTv() {
        return getFrameDuration() * frameCount;
    }

    public long getMediaTimescale() {
        return editRate.getNum();
    }

    public long getMediaDuration() {
        return frameCount * getFrameDuration();
    }

    public long getFrameDuration() {
        return editRate.getDen();
    }

    public int getSampleRate() {
        return 0;
    }

    public int getChannelCount() {
        return 0;
    }

//    public List<CAFChannelLabel> getChannelLabels() {
//        return new ArrayList<CAFChannelLabel>();
//    }

    public int getFrameCount() {
        return frameCount;
    }

    public PictureEssenceType getPictureEssenceType() {
        return pictureEssenceType;
    }

}
