package org.jcodec.containers.mp4.boxes.channel;

import static org.jcodec.common.model.Label.Ambisonic_W;
import static org.jcodec.common.model.Label.Ambisonic_X;
import static org.jcodec.common.model.Label.Ambisonic_Y;
import static org.jcodec.common.model.Label.Ambisonic_Z;
import static org.jcodec.common.model.Label.Center;
import static org.jcodec.common.model.Label.CenterSurround;
import static org.jcodec.common.model.Label.HeadphonesLeft;
import static org.jcodec.common.model.Label.HeadphonesRight;
import static org.jcodec.common.model.Label.LFE2;
import static org.jcodec.common.model.Label.LFEScreen;
import static org.jcodec.common.model.Label.Left;
import static org.jcodec.common.model.Label.LeftCenter;
import static org.jcodec.common.model.Label.LeftSurround;
import static org.jcodec.common.model.Label.LeftTotal;
import static org.jcodec.common.model.Label.MS_Mid;
import static org.jcodec.common.model.Label.MS_Side;
import static org.jcodec.common.model.Label.Mono;
import static org.jcodec.common.model.Label.RearSurroundLeft;
import static org.jcodec.common.model.Label.RearSurroundRight;
import static org.jcodec.common.model.Label.Right;
import static org.jcodec.common.model.Label.RightCenter;
import static org.jcodec.common.model.Label.RightSurround;
import static org.jcodec.common.model.Label.RightTotal;
import static org.jcodec.common.model.Label.TopBackCenter;
import static org.jcodec.common.model.Label.TopBackLeft;
import static org.jcodec.common.model.Label.TopBackRight;
import static org.jcodec.common.model.Label.TopCenterSurround;
import static org.jcodec.common.model.Label.XY_X;
import static org.jcodec.common.model.Label.XY_Y;

import org.jcodec.common.model.Label;

import js.util.ArrayList;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public final class ChannelLayout {
    private final static List<ChannelLayout> _values = new ArrayList<ChannelLayout>();

    public final static ChannelLayout kCAFChannelLayoutTag_UseChannelDescriptions = new ChannelLayout((0 << 16) | 0,
            new Label[] {});
    public final static ChannelLayout kCAFChannelLayoutTag_UseChannelBitmap = new ChannelLayout((1 << 16) | 0,
            new Label[] {});
    public final static ChannelLayout kCAFChannelLayoutTag_Mono = new ChannelLayout((100 << 16) | 1,
            new Label[] { Mono });
    public final static ChannelLayout kCAFChannelLayoutTag_Stereo = new ChannelLayout((101 << 16) | 2,
            new Label[] { Left, Right });
    public final static ChannelLayout kCAFChannelLayoutTag_StereoHeadphones = new ChannelLayout((102 << 16) | 2,
            new Label[] { HeadphonesLeft, HeadphonesRight });
    public final static ChannelLayout kCAFChannelLayoutTag_MatrixStereo = new ChannelLayout((103 << 16) | 2,
            new Label[] { LeftTotal, RightTotal });
    public final static ChannelLayout kCAFChannelLayoutTag_MidSide = new ChannelLayout((104 << 16) | 2,
            new Label[] { MS_Mid, MS_Side });
    public final static ChannelLayout kCAFChannelLayoutTag_XY = new ChannelLayout((105 << 16) | 2,
            new Label[] { XY_X, XY_Y });
    public final static ChannelLayout kCAFChannelLayoutTag_Binaural = new ChannelLayout((106 << 16) | 2,
            new Label[] { HeadphonesLeft, HeadphonesRight });
    public final static ChannelLayout kCAFChannelLayoutTag_Ambisonic_B_Format = new ChannelLayout((107 << 16) | 4,
            new Label[] { Ambisonic_W, Ambisonic_X, Ambisonic_Y, Ambisonic_Z });
    public final static ChannelLayout kCAFChannelLayoutTag_Quadraphonic = new ChannelLayout((108 << 16) | 4,
            new Label[] { Left, Right, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_Pentagonal = new ChannelLayout((109 << 16) | 5,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center });
    public final static ChannelLayout kCAFChannelLayoutTag_Hexagonal = new ChannelLayout((110 << 16) | 6,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_Octagonal = new ChannelLayout((111 << 16) | 8,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, CenterSurround, LeftCenter, RightCenter });
    public final static ChannelLayout kCAFChannelLayoutTag_Cube = new ChannelLayout((112 << 16) | 8, new Label[] { Left,
            Right, LeftSurround, RightSurround, TopBackLeft, TopBackRight, TopBackCenter, TopCenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_3_0_A = new ChannelLayout((113 << 16) | 3,
            new Label[] { Left, Right, Center });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_3_0_B = new ChannelLayout((114 << 16) | 3,
            new Label[] { Center, Left, Right });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_4_0_A = new ChannelLayout((115 << 16) | 4,
            new Label[] { Left, Right, Center, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_4_0_B = new ChannelLayout((116 << 16) | 4,
            new Label[] { Center, Left, Right, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_0_A = new ChannelLayout((117 << 16) | 5,
            new Label[] { Left, Right, Center, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_0_B = new ChannelLayout((118 << 16) | 5,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_0_C = new ChannelLayout((119 << 16) | 5,
            new Label[] { Left, Center, Right, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_0_D = new ChannelLayout((120 << 16) | 5,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_1_A = new ChannelLayout((121 << 16) | 6,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_1_B = new ChannelLayout((122 << 16) | 6,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_1_C = new ChannelLayout((123 << 16) | 6,
            new Label[] { Left, Center, Right, LeftSurround, RightSurround, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_5_1_D = new ChannelLayout((124 << 16) | 6,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_6_1_A = new ChannelLayout((125 << 16) | 7,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround, Right });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_7_1_A = new ChannelLayout((126 << 16) | 8,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround, LeftCenter, RightCenter });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_7_1_B = new ChannelLayout((127 << 16) | 8,
            new Label[] { Center, LeftCenter, RightCenter, Left, Right, LeftSurround, RightSurround, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_MPEG_7_1_C = new ChannelLayout((128 << 16) | 8, new Label[] {
            Left, Right, Center, LFEScreen, LeftSurround, RightSurround, RearSurroundLeft, RearSurroundRight });
    public final static ChannelLayout kCAFChannelLayoutTag_Emagic_Default_7_1 = new ChannelLayout((129 << 16) | 8,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, LFEScreen, LeftCenter, RightCenter });
    public final static ChannelLayout kCAFChannelLayoutTag_SMPTE_DTV = new ChannelLayout((130 << 16) | 8,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround, LeftTotal, RightTotal });
    public final static ChannelLayout kCAFChannelLayoutTag_ITU_2_1 = new ChannelLayout((131 << 16) | 3,
            new Label[] { Left, Right, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_ITU_2_2 = new ChannelLayout((132 << 16) | 4,
            new Label[] { Left, Right, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_DVD_4 = new ChannelLayout((133 << 16) | 3,
            new Label[] { Left, Right, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_DVD_5 = new ChannelLayout((134 << 16) | 4,
            new Label[] { Left, Right, LFEScreen, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_DVD_6 = new ChannelLayout((135 << 16) | 5,
            new Label[] { Left, Right, LFEScreen, LeftSurround, RightSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_DVD_10 = new ChannelLayout((136 << 16) | 4,
            new Label[] { Left, Right, Center, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_DVD_11 = new ChannelLayout((137 << 16) | 5,
            new Label[] { Left, Right, Center, LFEScreen, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_DVD_18 = new ChannelLayout((138 << 16) | 5,
            new Label[] { Left, Right, LeftSurround, RightSurround, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_AudioUnit_6_0 = new ChannelLayout((139 << 16) | 6,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_AudioUnit_7_0 = new ChannelLayout((140 << 16) | 7,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, RearSurroundLeft, RearSurroundRight });
    public final static ChannelLayout kCAFChannelLayoutTag_AAC_6_0 = new ChannelLayout((141 << 16) | 6,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_AAC_6_1 = new ChannelLayout((142 << 16) | 7,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, CenterSurround, LFEScreen });
    public final static ChannelLayout kCAFChannelLayoutTag_AAC_7_0 = new ChannelLayout((143 << 16) | 7,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, RearSurroundLeft, RearSurroundRight });
    public final static ChannelLayout kCAFChannelLayoutTag_AAC_Octagonal = new ChannelLayout((144 << 16) | 8,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, RearSurroundLeft, RearSurroundRight,
                    CenterSurround });
    public final static ChannelLayout kCAFChannelLayoutTag_TMH_10_2_std = new ChannelLayout((145 << 16) | 16,
            new Label[] { Left, Right, Center, Mono, Mono, Mono, LeftSurround, RightSurround, Mono, Mono, Mono, Mono,
                    Mono, CenterSurround, LFEScreen, LFE2 });
    public final static ChannelLayout kCAFChannelLayoutTag_TMH_10_2_full = new ChannelLayout((146 << 16) | 21,
            new Label[] { LeftCenter, RightCenter, Mono, Mono, Mono });
    public final static ChannelLayout kCAFChannelLayoutTag_RESERVED_DO_NOT_USE = new ChannelLayout((147 << 16),
            new Label[0]);

    private int code;
    private Label[] labels;

    private ChannelLayout(int code, Label[] labels) {
        this.code = code;
        this.labels = labels;
        _values.add(this);
    }

    public int getCode() {
        return code;
    }

    public Label[] getLabels() {
        return labels;
    }

    public static ChannelLayout[] values() {
        return _values.toArray(new ChannelLayout[0]);
    }
}