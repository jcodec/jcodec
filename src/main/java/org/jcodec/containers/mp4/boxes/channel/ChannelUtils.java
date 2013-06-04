package org.jcodec.containers.mp4.boxes.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChannelBox;
import org.jcodec.containers.mp4.boxes.ChannelBox.ChannelDescription;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ChannelUtils {

    private static final List<Label> MONO = Arrays.asList(Label.Mono);
    private static final List<Label> STEREO = Arrays.asList(Label.Left, Label.Right);
    private static final List<Label> MATRIX_STEREO = Arrays.asList(Label.LeftTotal, Label.RightTotal);
    private static final Label[] EMPTY = new Label[0];

    public static Label[] getLabels(AudioSampleEntry se) {
        ChannelBox channel = Box.findFirst(se, ChannelBox.class, "chan");
        if (channel != null)
            return ChannelUtils.getLabels(channel);
        else {
            short channelCount = se.getChannelCount();
            switch (channelCount) {
            case 1:
                return new Label[] { Label.Mono };
            case 2:
                return new Label[] { Label.Left, Label.Right };
            case 3:
                return new Label[] { Label.Left, Label.Right, Label.Center };
            case 4:
                return new Label[] { Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround };
            case 5:
                return new Label[] { Label.Left, Label.Right, Label.Center, Label.LeftSurround, Label.RightSurround };
            case 6:
                return new Label[] { Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround,
                        Label.RightSurround };
            default:
                Label[] res = new Label[channelCount];
                Arrays.fill(res, Label.Mono);
                return res;
            }
        }
    }

    public static Label[] getLabels(TrakBox trakBox) {
        return getLabels((AudioSampleEntry) trakBox.getSampleEntries()[0]);
    }

    public static void setLabel(TrakBox trakBox, int channel, Label label) {
        Label[] labels = getLabels(trakBox);
        labels[channel] = label;
        setLabels(trakBox, labels);
    }

    private static void setLabels(TrakBox trakBox, Label[] labels) {
        ChannelBox channel = Box.findFirst(trakBox, ChannelBox.class, "mdia", "minf", "stbl", "stsd", null, "chan");
        if (channel == null) {
            channel = new ChannelBox();
            Box.findFirst(trakBox, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null).add(channel);
        }
        channel.setChannelLayout(ChannelLayout.kCAFChannelLayoutTag_UseChannelDescriptions.getCode());
        ChannelDescription[] list = new ChannelDescription[labels.length];
        for (int i = 0; i < labels.length; i++)
            list[i] = new ChannelBox.ChannelDescription(labels[i].getVal(), 0, new float[] { 0, 0, 0 });
        channel.setDescriptions(list);
    }

    public static Label[] getLabels(ChannelBox box) {
        long tag = box.getChannelLayout();
        for (ChannelLayout layout : EnumSet.allOf(ChannelLayout.class)) {
            if (layout.getCode() == tag) {
                switch (layout) {
                case kCAFChannelLayoutTag_UseChannelDescriptions:
                    return extractLabels(box.getDescriptions());
                case kCAFChannelLayoutTag_UseChannelBitmap:
                    return getLabelsByBitmap(box.getChannelBitmap());
                default:
                    return layout.getLabels();
                }
            }
        }
        return EMPTY;
    }

    private static Label[] extractLabels(ChannelDescription[] descriptions) {
        Label[] result = new Label[descriptions.length];
        for (int i = 0; i < descriptions.length; i++)
            result[i] = descriptions[i].getLabel();
        return result;
    }

    /**
     * <code>
        enum
        {
            kCAFChannelBit_Left                 = (1<<0),
            kCAFChannelBit_Right                = (1<<1),
            kCAFChannelBit_Center               = (1<<2),
            kCAFChannelBit_LFEScreen            = (1<<3),
            kCAFChannelBit_LeftSurround         = (1<<4),   // WAVE: "Back Left"
            kCAFChannelBit_RightSurround        = (1<<5),   // WAVE: "Back Right"
            kCAFChannelBit_LeftCenter           = (1<<6),
            kCAFChannelBit_RightCenter          = (1<<7),
            kCAFChannelBit_CenterSurround       = (1<<8),   // WAVE: "Back Center"
            kCAFChannelBit_LeftSurroundDirect   = (1<<9),   // WAVE: "Side Left"
            kCAFChannelBit_RightSurroundDirect  = (1<<10), // WAVE: "Side Right"
            kCAFChannelBit_TopCenterSurround    = (1<<11),
            kCAFChannelBit_VerticalHeightLeft   = (1<<12), // WAVE: "Top Front Left"
            kCAFChannelBit_VerticalHeightCenter = (1<<13), // WAVE: "Top Front Center"
            kCAFChannelBit_VerticalHeightRight  = (1<<14), // WAVE: "Top Front Right"
            kCAFChannelBit_TopBackLeft          = (1<<15),
            kCAFChannelBit_TopBackCenter        = (1<<16),
            kCAFChannelBit_TopBackRight         = (1<<17)
        };
        </code>
     * 
     * @param channelBitmap
     * @return
     */
    public static Label[] getLabelsByBitmap(long channelBitmap) {
        List<Label> result = new ArrayList<Label>();
        for (Label label : Label.values()) {
            if ((label.bitmapVal & channelBitmap) != 0)
                result.add(label);
        }
        return result.toArray(new Label[0]);
    }
}