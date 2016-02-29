package org.jcodec.containers.mp4.boxes.channel;

import static org.jcodec.containers.mp4.boxes.channel.Label.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum ChannelLayout {
    kCAFChannelLayoutTag_UseChannelDescriptions((0 << 16) | 0, new Label[] {}),

    kCAFChannelLayoutTag_UseChannelBitmap((1 << 16) | 0, new Label[] {}),

    kCAFChannelLayoutTag_Mono((100 << 16) | 1, new Label[] { Mono }),

    kCAFChannelLayoutTag_Stereo((101 << 16) | 2, new Label[] { Left, Right }),

    kCAFChannelLayoutTag_StereoHeadphones((102 << 16) | 2, new Label[] { HeadphonesLeft, HeadphonesRight }),

    kCAFChannelLayoutTag_MatrixStereo((103 << 16) | 2, new Label[] { LeftTotal, RightTotal }),

    kCAFChannelLayoutTag_MidSide((104 << 16) | 2, new Label[] { MS_Mid, MS_Side }),

    kCAFChannelLayoutTag_XY((105 << 16) | 2, new Label[] { XY_X, XY_Y }),

    kCAFChannelLayoutTag_Binaural((106 << 16) | 2, new Label[] { HeadphonesLeft, HeadphonesRight }),

    kCAFChannelLayoutTag_Ambisonic_B_Format((107 << 16) | 4,
            new Label[] { Ambisonic_W, Ambisonic_X, Ambisonic_Y, Ambisonic_Z }),

    kCAFChannelLayoutTag_Quadraphonic((108 << 16) | 4, new Label[] { Left, Right, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_Pentagonal((109 << 16) | 5, new Label[] { Left, Right, LeftSurround, RightSurround, Center }),

    kCAFChannelLayoutTag_Hexagonal((110 << 16) | 6,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, CenterSurround }),

    kCAFChannelLayoutTag_Octagonal((111 << 16) | 8,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, CenterSurround, LeftCenter, RightCenter }),

    kCAFChannelLayoutTag_Cube((112 << 16) | 8, new Label[] { Left, Right, LeftSurround, RightSurround, TopBackLeft,
            TopBackRight, TopBackCenter, TopCenterSurround }),

    kCAFChannelLayoutTag_MPEG_3_0_A((113 << 16) | 3, new Label[] { Left, Right, Center }),

    kCAFChannelLayoutTag_MPEG_3_0_B((114 << 16) | 3, new Label[] { Center, Left, Right }),

    kCAFChannelLayoutTag_MPEG_4_0_A((115 << 16) | 4, new Label[] { Left, Right, Center, CenterSurround }),

    kCAFChannelLayoutTag_MPEG_4_0_B((116 << 16) | 4, new Label[] { Center, Left, Right, CenterSurround }),

    kCAFChannelLayoutTag_MPEG_5_0_A((117 << 16) | 5, new Label[] { Left, Right, Center, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_MPEG_5_0_B((118 << 16) | 5, new Label[] { Left, Right, LeftSurround, RightSurround, Center }),

    kCAFChannelLayoutTag_MPEG_5_0_C((119 << 16) | 5, new Label[] { Left, Center, Right, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_MPEG_5_0_D((120 << 16) | 5, new Label[] { Center, Left, Right, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_MPEG_5_1_A((121 << 16) | 6,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_MPEG_5_1_B((122 << 16) | 6,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, LFEScreen }),

    kCAFChannelLayoutTag_MPEG_5_1_C((123 << 16) | 6,
            new Label[] { Left, Center, Right, LeftSurround, RightSurround, LFEScreen }),

    kCAFChannelLayoutTag_MPEG_5_1_D((124 << 16) | 6,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, LFEScreen }),

    kCAFChannelLayoutTag_MPEG_6_1_A((125 << 16) | 7,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround, Right }),

    kCAFChannelLayoutTag_MPEG_7_1_A((126 << 16) | 8,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround, LeftCenter, RightCenter }),

    kCAFChannelLayoutTag_MPEG_7_1_B((127 << 16) | 8,
            new Label[] { Center, LeftCenter, RightCenter, Left, Right, LeftSurround, RightSurround, LFEScreen }),

    kCAFChannelLayoutTag_MPEG_7_1_C((128 << 16) | 8, new Label[] { Left, Right, Center, LFEScreen, LeftSurround,
            RightSurround, RearSurroundLeft, RearSurroundRight }),

    kCAFChannelLayoutTag_Emagic_Default_7_1((129 << 16) | 8,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, LFEScreen, LeftCenter, RightCenter }),

    kCAFChannelLayoutTag_SMPTE_DTV((130 << 16) | 8,
            new Label[] { Left, Right, Center, LFEScreen, LeftSurround, RightSurround, LeftTotal, RightTotal }),

    kCAFChannelLayoutTag_ITU_2_1((131 << 16) | 3, new Label[] { Left, Right, CenterSurround }),

    kCAFChannelLayoutTag_ITU_2_2((132 << 16) | 4, new Label[] { Left, Right, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_DVD_4((133 << 16) | 3, new Label[] { Left, Right, LFEScreen }),

    kCAFChannelLayoutTag_DVD_5((134 << 16) | 4, new Label[] { Left, Right, LFEScreen, CenterSurround }),

    kCAFChannelLayoutTag_DVD_6((135 << 16) | 5, new Label[] { Left, Right, LFEScreen, LeftSurround, RightSurround }),

    kCAFChannelLayoutTag_DVD_10((136 << 16) | 4, new Label[] { Left, Right, Center, LFEScreen }),

    kCAFChannelLayoutTag_DVD_11((137 << 16) | 5, new Label[] { Left, Right, Center, LFEScreen, CenterSurround }),

    kCAFChannelLayoutTag_DVD_18((138 << 16) | 5, new Label[] { Left, Right, LeftSurround, RightSurround, LFEScreen }),

    kCAFChannelLayoutTag_AudioUnit_6_0((139 << 16) | 6,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, CenterSurround }),

    kCAFChannelLayoutTag_AudioUnit_7_0((140 << 16) | 7,
            new Label[] { Left, Right, LeftSurround, RightSurround, Center, RearSurroundLeft, RearSurroundRight }),

    kCAFChannelLayoutTag_AAC_6_0((141 << 16) | 6,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, CenterSurround }),

    kCAFChannelLayoutTag_AAC_6_1((142 << 16) | 7,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, CenterSurround, LFEScreen }),

    kCAFChannelLayoutTag_AAC_7_0((143 << 16) | 7,
            new Label[] { Center, Left, Right, LeftSurround, RightSurround, RearSurroundLeft, RearSurroundRight }),

    kCAFChannelLayoutTag_AAC_Octagonal((144 << 16) | 8, new Label[] { Center, Left, Right, LeftSurround, RightSurround,
            RearSurroundLeft, RearSurroundRight, CenterSurround }),

    kCAFChannelLayoutTag_TMH_10_2_std((145 << 16) | 16, new Label[] { Left, Right, Center, Mono, Mono, Mono,
            LeftSurround, RightSurround, Mono, Mono, Mono, Mono, Mono, CenterSurround, LFEScreen, LFE2 }),

    kCAFChannelLayoutTag_TMH_10_2_full((146 << 16) | 21, new Label[] { LeftCenter, RightCenter, Mono, Mono, Mono }),

    kCAFChannelLayoutTag_RESERVED_DO_NOT_USE((147 << 16), new Label[0]);

    private int code;
    private Label[] labels;

    private ChannelLayout(int code, Label[] labels) {
        this.code = code;
        this.labels = labels;
    }

    public int getCode() {
        return code;
    }

    public Label[] getLabels() {
        return labels;
    }
}