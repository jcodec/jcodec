package org.jcodec.containers.mp4.boxes.channel;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum ChannelLayout {
    kCAFChannelLayoutTag_UseChannelDescriptions((0 << 16) | 0),

    kCAFChannelLayoutTag_UseChannelBitmap((1 << 16) | 0),

    kCAFChannelLayoutTag_Mono((100 << 16) | 1),

    kCAFChannelLayoutTag_Stereo((101 << 16) | 2, Label.Left, Label.Right),

    kCAFChannelLayoutTag_StereoHeadphones((102 << 16) | 2, Label.HeadphonesLeft, Label.HeadphonesRight),

    kCAFChannelLayoutTag_MatrixStereo((103 << 16) | 2, Label.LeftTotal, Label.RightTotal),

    kCAFChannelLayoutTag_MidSide((104 << 16) | 2, Label.MS_Mid, Label.MS_Side),

    kCAFChannelLayoutTag_XY((105 << 16) | 2, Label.XY_X, Label.XY_Y),

    kCAFChannelLayoutTag_Binaural((106 << 16) | 2, Label.HeadphonesLeft, Label.HeadphonesRight),

    kCAFChannelLayoutTag_Ambisonic_B_Format((107 << 16) | 4, Label.Ambisonic_W, Label.Ambisonic_X, Label.Ambisonic_Y,
            Label.Ambisonic_Z),

    kCAFChannelLayoutTag_Quadraphonic((108 << 16) | 4, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround),

    kCAFChannelLayoutTag_Pentagonal((109 << 16) | 5, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.Center),

    kCAFChannelLayoutTag_Hexagonal((110 << 16) | 6, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.Center, Label.CenterSurround),

    kCAFChannelLayoutTag_Octagonal((111 << 16) | 8, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.Center, Label.CenterSurround, Label.LeftCenter, Label.RightCenter),

    kCAFChannelLayoutTag_Cube((112 << 16) | 8, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.TopBackLeft, Label.TopBackRight, Label.TopBackCenter, Label.TopCenterSurround),

    kCAFChannelLayoutTag_MPEG_3_0_A((113 << 16) | 3, Label.Left, Label.Right, Label.Center),

    kCAFChannelLayoutTag_MPEG_3_0_B((114 << 16) | 3, Label.Center, Label.Left, Label.Right),

    kCAFChannelLayoutTag_MPEG_4_0_A((115 << 16) | 4, Label.Left, Label.Right, Label.Center, Label.CenterSurround),

    kCAFChannelLayoutTag_MPEG_4_0_B((116 << 16) | 4, Label.Center, Label.Left, Label.Right, Label.CenterSurround),

    kCAFChannelLayoutTag_MPEG_5_0_A((117 << 16) | 5, Label.Left, Label.Right, Label.Center, Label.LeftSurround,
            Label.RightSurround),

    kCAFChannelLayoutTag_MPEG_5_0_B((118 << 16) | 5, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.Center),

    kCAFChannelLayoutTag_MPEG_5_0_C((119 << 16) | 5, Label.Left, Label.Center, Label.Right, Label.LeftSurround,
            Label.RightSurround),

    kCAFChannelLayoutTag_MPEG_5_0_D((120 << 16) | 5, Label.Center, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround),

    kCAFChannelLayoutTag_MPEG_5_1_A((121 << 16) | 6, Label.Left, Label.Right, Label.Center, Label.LFEScreen,
            Label.LeftSurround, Label.RightSurround),

    kCAFChannelLayoutTag_MPEG_5_1_B((122 << 16) | 6, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.Center, Label.LFEScreen),

    kCAFChannelLayoutTag_MPEG_5_1_C((123 << 16) | 6, Label.Left, Label.Center, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.LFEScreen),

    kCAFChannelLayoutTag_MPEG_5_1_D((124 << 16) | 6, Label.Center, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.LFEScreen),

    kCAFChannelLayoutTag_MPEG_6_1_A((125 << 16) | 7, Label.Left, Label.Right, Label.Center, Label.LFEScreen,
            Label.LeftSurround, Label.RightSurround, Label.Right),

    kCAFChannelLayoutTag_MPEG_7_1_A((126 << 16) | 8, Label.Left, Label.Right, Label.Center, Label.LFEScreen,
            Label.LeftSurround, Label.RightSurround, Label.LeftCenter, Label.RightCenter),

    kCAFChannelLayoutTag_MPEG_7_1_B((127 << 16) | 8, Label.Center, Label.LeftCenter, Label.RightCenter, Label.Left,
            Label.Right, Label.LeftSurround, Label.RightSurround, Label.LFEScreen),

    kCAFChannelLayoutTag_MPEG_7_1_C((128 << 16) | 8, Label.Left, Label.Right, Label.Center, Label.LFEScreen,
            Label.LeftSurround, Label.RightSurround, Label.RearSurroundLeft, Label.RearSurroundRight),

    kCAFChannelLayoutTag_Emagic_Default_7_1((129 << 16) | 8, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.Center, Label.LFEScreen, Label.LeftCenter, Label.RightCenter),

    kCAFChannelLayoutTag_SMPTE_DTV((130 << 16) | 8, Label.Left, Label.Right, Label.Center, Label.LFEScreen,
            Label.LeftSurround, Label.RightSurround, Label.LeftTotal, Label.RightTotal),

    kCAFChannelLayoutTag_ITU_2_1((131 << 16) | 3, Label.Left, Label.Right, Label.CenterSurround),

    kCAFChannelLayoutTag_ITU_2_2((132 << 16) | 4, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround),

    kCAFChannelLayoutTag_DVD_4((133 << 16) | 3, Label.Left, Label.Right, Label.LFEScreen),

    kCAFChannelLayoutTag_DVD_5((134 << 16) | 4, Label.Left, Label.Right, Label.LFEScreen, Label.CenterSurround),

    kCAFChannelLayoutTag_DVD_6((135 << 16) | 5, Label.Left, Label.Right, Label.LFEScreen, Label.LeftSurround,
            Label.RightSurround),

    kCAFChannelLayoutTag_DVD_10((136 << 16) | 4, Label.Left, Label.Right, Label.Center, Label.LFEScreen),

    kCAFChannelLayoutTag_DVD_11((137 << 16) | 5, Label.Left, Label.Right, Label.Center, Label.LFEScreen,
            Label.CenterSurround),

    kCAFChannelLayoutTag_DVD_18((138 << 16) | 5, Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround,
            Label.LFEScreen),

    kCAFChannelLayoutTag_AudioUnit_6_0((139 << 16) | 6, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.Center, Label.CenterSurround),

    kCAFChannelLayoutTag_AudioUnit_7_0((140 << 16) | 7, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.Center, Label.RearSurroundLeft, Label.RearSurroundRight),

    kCAFChannelLayoutTag_AAC_6_0((141 << 16) | 6, Label.Center, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.CenterSurround),

    kCAFChannelLayoutTag_AAC_6_1((142 << 16) | 7, Label.Center, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.CenterSurround, Label.LFEScreen),

    kCAFChannelLayoutTag_AAC_7_0((143 << 16) | 7, Label.Center, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.RearSurroundLeft, Label.RearSurroundRight),

    kCAFChannelLayoutTag_AAC_Octagonal((144 << 16) | 8, Label.Center, Label.Left, Label.Right, Label.LeftSurround,
            Label.RightSurround, Label.RearSurroundLeft, Label.RearSurroundRight, Label.CenterSurround),

    kCAFChannelLayoutTag_TMH_10_2_std((145 << 16) | 16, Label.Left, Label.Right, Label.Center, Label.Mono, Label.Mono,
            Label.Mono, Label.LeftSurround, Label.RightSurround, Label.Mono, Label.Mono, Label.Mono, Label.Mono,
            Label.Mono, Label.CenterSurround, Label.LFEScreen, Label.LFE2),

    kCAFChannelLayoutTag_TMH_10_2_full((146 << 16) | 21, Label.LeftCenter, Label.RightCenter, Label.Mono, Label.Mono,
            Label.Mono),

    kCAFChannelLayoutTag_RESERVED_DO_NOT_USE((147 << 16));

    private int code;
    private Label[] labels;

    private ChannelLayout(int code, Label... labels) {
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