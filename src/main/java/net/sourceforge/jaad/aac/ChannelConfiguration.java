package net.sourceforge.jaad.aac;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * All possible channel configurations for AAC.
 * 
 * @author in-somnia
 */
public final class ChannelConfiguration {

    public final static ChannelConfiguration CHANNEL_CONFIG_UNSUPPORTED = new ChannelConfiguration(-1, "invalid");
    public final static ChannelConfiguration CHANNEL_CONFIG_NONE = new ChannelConfiguration(0, "No channel");
    public final static ChannelConfiguration CHANNEL_CONFIG_MONO = new ChannelConfiguration(1, "Mono");
    public final static ChannelConfiguration CHANNEL_CONFIG_STEREO = new ChannelConfiguration(2, "Stereo");
    public final static ChannelConfiguration CHANNEL_CONFIG_STEREO_PLUS_CENTER = new ChannelConfiguration(3,
            "Stereo+Center");
    public final static ChannelConfiguration CHANNEL_CONFIG_STEREO_PLUS_CENTER_PLUS_REAR_MONO = new ChannelConfiguration(
            4, "Stereo+Center+Rear");
    public final static ChannelConfiguration CHANNEL_CONFIG_FIVE = new ChannelConfiguration(5, "Five channels");
    public final static ChannelConfiguration CHANNEL_CONFIG_FIVE_PLUS_ONE = new ChannelConfiguration(6,
            "Five channels+LF");
    public final static ChannelConfiguration CHANNEL_CONFIG_SEVEN_PLUS_ONE = new ChannelConfiguration(8,
            "Seven channels+LF");

    public static ChannelConfiguration forInt(int i) {
        ChannelConfiguration c;
        switch (i) {
        case 0:
            c = CHANNEL_CONFIG_NONE;
            break;
        case 1:
            c = CHANNEL_CONFIG_MONO;
            break;
        case 2:
            c = CHANNEL_CONFIG_STEREO;
            break;
        case 3:
            c = CHANNEL_CONFIG_STEREO_PLUS_CENTER;
            break;
        case 4:
            c = CHANNEL_CONFIG_STEREO_PLUS_CENTER_PLUS_REAR_MONO;
            break;
        case 5:
            c = CHANNEL_CONFIG_FIVE;
            break;
        case 6:
            c = CHANNEL_CONFIG_FIVE_PLUS_ONE;
            break;
        case 7:
        case 8:
            c = CHANNEL_CONFIG_SEVEN_PLUS_ONE;
            break;
        default:
            c = CHANNEL_CONFIG_UNSUPPORTED;
            break;
        }
        return c;
    }

    private final int chCount;
    private final String descr;

    private ChannelConfiguration(int chCount, String descr) {
        this.chCount = chCount;
        this.descr = descr;
    }

    /**
     * Returns the number of channels in this configuration.
     */
    public int getChannelCount() {
        return chCount;
    }

    /**
     * Returns a short description of this configuration.
     * 
     * @return the channel configuration's description
     */
    public String getDescription() {
        return descr;
    }

    /**
     * Returns a string representation of this channel configuration. The method
     * is identical to <code>getDescription()</code>.
     * 
     * @return the channel configuration's description
     */
    @Override
    public String toString() {
        return descr;
    }
}
