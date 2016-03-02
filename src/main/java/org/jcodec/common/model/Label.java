package org.jcodec.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public final class Label {
    private final static List<Label> _values = new ArrayList<Label>();

/** unknown role or unspecified other use for channel */
public final static Label Unknown = new Label(0xFFFFFFFF);

/** channel is present, but has no intended role or destination */
public final static Label Unused = new Label(0);

/** channel is described solely by the mCoordinates fields */
public final static Label     UseCoordinates = new Label(100);

public final static Label     Left = new Label(1);
public final static Label  Right = new Label(2);
public final static Label  Center = new Label(3);
public final static Label  LFEScreen = new Label(4);

    /** WAVE (.wav files): "Back Left" */
public final static Label LeftSurround = new Label(5);

/** WAVE: "Back Right" */
public final static Label     RightSurround = new Label(6);
public final static Label  LeftCenter = new Label(7);
public final static Label  RightCenter = new Label(8);

    /** WAVE: "Back  Center or  plain "Rear Surround" */
public final static Label CenterSurround = new Label(9);

/** WAVE: "Side Left" */
public final static Label LeftSurroundDirect = new Label(10);

/** WAVE: "Side Right" */
public final static Label     RightSurroundDirect = new Label(11);
public final static Label  TopCenterSurround = new Label(12);

    /** WAVE: "Top Front Left" */
public final static Label VerticalHeightLeft = new Label(13);

/** WAVE: "Top Front Center" */
public final static Label VerticalHeightCenter = new Label(14);

/** WAVE: "Top Front Right" */
public final static Label     VerticalHeightRight = new Label(15);
public final static Label  TopBackLeft = new Label(16);
public final static Label  TopBackCenter = new Label(17);
public final static Label  TopBackRight = new Label(18);

public final static Label     RearSurroundLeft = new Label(33);
public final static Label  RearSurroundRight = new Label(34);
public final static Label  LeftWide = new Label(35);
public final static Label  RightWide = new Label(36);
public final static Label  LFE2 = new Label(37);

    /** matrix encoded 4 channels */
public final static Label LeftTotal = new Label(38);

/** matrix encoded 4 channels */
public final static Label     RightTotal = new Label(39);
public final static Label  HearingImpaired = new Label(40);
public final static Label  Narration = new Label(41);
public final static Label  Mono = new Label(42);
public final static Label  DialogCentricMix = new Label(43);

    /** center, non diffuse first order ambisonic channels */
public final static Label     CenterSurroundDirect = new Label(44);
public final static Label  Ambisonic_W = new Label(200);
public final static Label  Ambisonic_X = new Label(201);
public final static Label  Ambisonic_Y = new Label(202);
public final static Label  Ambisonic_Z = new Label(203);

    /** Mid/Side Recording */
public final static Label     MS_Mid = new Label(204);
public final static Label  MS_Side = new Label(205);

    /** X-Y Recording */
public final static Label     XY_X = new Label(206);
public final static Label  XY_Y = new Label(207);

public final static Label     HeadphonesLeft = new Label(301);
public final static Label  HeadphonesRight = new Label(302);
public final static Label  ClickTrack = new Label(304);
public final static Label  ForeignLanguage = new Label(305);

    // generic discrete channel
public final static Label Discrete               = new Label( 400);

// numbered discrete channel
public final static Label Discrete_0             = new Label( (1<<16) | 0);

public final static Label Discrete_1             = new Label( (1<<16) | 1);

public final static Label Discrete_2             = new Label( (1<<16) | 2);

public final static Label Discrete_3             = new Label( (1<<16) | 3);

public final static Label Discrete_4             = new Label( (1<<16) | 4);

public final static Label Discrete_5             = new Label( (1<<16) | 5);

public final static Label Discrete_6             = new Label( (1<<16) | 6);

public final static Label Discrete_7             = new Label( (1<<16) | 7);

public final static Label Discrete_8             = new Label( (1<<16) | 8);

public final static Label Discrete_9             = new Label( (1<<16) | 9);

public final static Label Discrete_10            = new Label( (1<<16) | 10);

public final static Label Discrete_11            = new Label( (1<<16) | 11);

public final static Label Discrete_12            = new Label( (1<<16) | 12);

public final static Label Discrete_13            = new Label( (1<<16) | 13);

public final static Label Discrete_14            = new Label( (1<<16) | 14);

public final static Label Discrete_15            = new Label( (1<<16) | 15);

public final static Label Discrete_65535         = new Label( (1<<16) | 65535);
    
    final int labelVal;
    public final long bitmapVal;
    public final static Pattern channelMappingRegex = Pattern.compile("[_\\ \\.][a-zA-Z]+$");

    private Label(int val) {
        this.labelVal = val;
        this.bitmapVal = (this.labelVal > 18 || this.labelVal < 1) ? 0x00000000 : 1 << (this.labelVal - 1);
        _values.add(this);
    }
    
    public static Label[] values() {
        return _values.toArray(new Label[0]);
    }

    public static Label getByVal(int val) {
        Label[] values = Label.values();
        for (int i = 0; i < values.length; i++) {
            Label label = values[i];
            if (label.labelVal == val)
                return label;
        }
        return Label.Mono;
    }
    
    public int getVal() {
        return labelVal;
    }
}