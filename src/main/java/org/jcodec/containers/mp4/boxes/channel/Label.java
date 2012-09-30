package org.jcodec.containers.mp4.boxes.channel;

import java.util.regex.Pattern;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum Label {
    /** unknown role or unspecified other use for channel */
    Unknown(0xFFFFFFFF),
    /** channel is present, but has no intended role or destination */
    Unused(0),
    /** channel is described solely by the mCoordinates fields */
    UseCoordinates(100),

    Left(1), Right(2), Center(3), LFEScreen(4),

    /** WAVE (.wav files): "Back Left" */
    LeftSurround(5),
    /** WAVE: "Back Right" */
    RightSurround(6), LeftCenter(7), RightCenter(8),
    /** WAVE: "Back  Center or  plain "Rear Surround" */
    CenterSurround(9),
    /** WAVE: "Side Left" */
    LeftSurroundDirect(10),
    /** WAVE: "Side Right" */
    RightSurroundDirect(11), TopCenterSurround(12),
    /** WAVE: "Top Front Left" */
    VerticalHeightLeft(13),
    /** WAVE: "Top Front Center" */
    VerticalHeightCenter(14),
    /** WAVE: "Top Front Right" */
    VerticalHeightRight(15), TopBackLeft(16), TopBackCenter(17), TopBackRight(18),

    RearSurroundLeft(33), RearSurroundRight(34), LeftWide(35), RightWide(36), LFE2(37),
    /** matrix encoded 4 channels */
    LeftTotal(38),
    /** matrix encoded 4 channels */
    RightTotal(39), HearingImpaired(40), Narration(41), Mono(42), DialogCentricMix(43),

    /** center, non diffuse first order ambisonic channels */
    CenterSurroundDirect(44), Ambisonic_W(200), Ambisonic_X(201), Ambisonic_Y(202), Ambisonic_Z(203),

    /** Mid/Side Recording */
    MS_Mid(204), MS_Side(205),

    /** X-Y Recording */
    XY_X(206), XY_Y(207),

    HeadphonesLeft(301), HeadphonesRight(302), ClickTrack(304), ForeignLanguage(305),
    // generic discrete channel
    Discrete              ( 400),
    
    // numbered discrete channel
    Discrete_0            ( (1<<16) | 0),
    Discrete_1            ( (1<<16) | 1),
    Discrete_2            ( (1<<16) | 2),
    Discrete_3            ( (1<<16) | 3),
    Discrete_4            ( (1<<16) | 4),
    Discrete_5            ( (1<<16) | 5),
    Discrete_6            ( (1<<16) | 6),
    Discrete_7            ( (1<<16) | 7),
    Discrete_8            ( (1<<16) | 8),
    Discrete_9            ( (1<<16) | 9),
    Discrete_10           ( (1<<16) | 10),
    Discrete_11           ( (1<<16) | 11),
    Discrete_12           ( (1<<16) | 12),
    Discrete_13           ( (1<<16) | 13),
    Discrete_14           ( (1<<16) | 14),
    Discrete_15           ( (1<<16) | 15),
    Discrete_65535        ( (1<<16) | 65535);
    
    final int labelVal;
    final long bitmapVal;
    public final static Pattern channelMappingRegex = Pattern.compile("[_\\ \\.][a-zA-Z]+$");

    private Label(int val) {
        this.labelVal = val;
        this.bitmapVal = (this.labelVal > 18 || this.labelVal < 1) ? 0x00000000 : 1 << (this.labelVal - 1);

    }

    public static Label getByVal(int val) {
        for (Label label : Label.values()) {
            if (label.labelVal == val)
                return label;
        }
        return Label.Mono;
    }
    
    public int getVal() {
        return labelVal;
    }
}