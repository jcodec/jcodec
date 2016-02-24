package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.channel.ChannelUtils;
import org.jcodec.containers.mp4.boxes.channel.Label;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Describes audio payload sample
 * 
 * @author The JCodec project
 * 
 */
public class AudioSampleEntry extends SampleEntry {

    //@formatter:off
    public static int kAudioFormatFlagIsFloat = 0x1;
    public static int kAudioFormatFlagIsBigEndian = 0x2;
    public static int kAudioFormatFlagIsSignedInteger = 0x4;
    public static int kAudioFormatFlagIsPacked = 0x8;
    public static int kAudioFormatFlagIsAlignedHigh = 0x10;
    public static int kAudioFormatFlagIsNonInterleaved = 0x20;
    public static int kAudioFormatFlagIsNonMixable = 0x40;
    //@formatter:on    

    private static final MyFactory FACTORY = new MyFactory();

    public static AudioSampleEntry createAudioSampleEntry(Header header, short drefInd, short channelCount,
            short sampleSize, int sampleRate, short revision, int vendor, int compressionId, int pktSize,
            int samplesPerPkt, int bytesPerPkt, int bytesPerFrame, int bytesPerSample, short version) {
        AudioSampleEntry audio = new AudioSampleEntry(header);
        audio.drefInd = drefInd;
        audio.channelCount = channelCount;
        audio.sampleSize = sampleSize;
        audio.sampleRate = sampleRate;
        audio.revision = revision;
        audio.vendor = vendor;
        audio.compressionId = compressionId;
        audio.pktSize = pktSize;
        audio.samplesPerPkt = samplesPerPkt;
        audio.bytesPerPkt = bytesPerPkt;
        audio.bytesPerFrame = bytesPerFrame;
        audio.bytesPerSample = bytesPerSample;
        audio.version = version;
        return audio;
    }

    private short channelCount;
    private short sampleSize;
    private float sampleRate;

    private short revision;
    private int vendor;
    private int compressionId;
    private int pktSize;
    private int samplesPerPkt;
    private int bytesPerPkt;
    private int bytesPerFrame;
    private int bytesPerSample;
    private short version;
    private int lpcmFlags;

    public AudioSampleEntry(Header atom) {
        super(atom);
        factory = FACTORY;
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        version = input.getShort();
        revision = input.getShort();
        vendor = input.getInt();

        channelCount = input.getShort();
        sampleSize = input.getShort();

        compressionId = input.getShort();
        pktSize = input.getShort();

        long sr = input.getInt() & 0xffffffffL;
        sampleRate = (float) sr / 65536f;

        if (version == 1) {
            samplesPerPkt = input.getInt();
            bytesPerPkt = input.getInt();
            bytesPerFrame = input.getInt();
            bytesPerSample = input.getInt();
        } else if (version == 2) {
            input.getInt(); /* sizeof struct only */
            sampleRate = (float) Double.longBitsToDouble(input.getLong());
            channelCount = (short) input.getInt();
            input.getInt(); /* always 0x7F000000 */
            sampleSize = (short) input.getInt();
            lpcmFlags = (int) input.getInt();
            bytesPerFrame = (int) input.getInt();
            samplesPerPkt = (int) input.getInt();
        }
        parseExtensions(input);
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        out.putShort(version);
        out.putShort(revision);
        out.putInt(vendor);

        if (version < 2) {
            out.putShort(channelCount);
            if (version == 0)
                out.putShort(sampleSize);
            else
                out.putShort((short) 16);

            out.putShort((short) compressionId);
            out.putShort((short) pktSize);

            out.putInt((int) Math.round(sampleRate * 65536d));

            if (version == 1) {
                out.putInt(samplesPerPkt);
                out.putInt(bytesPerPkt);
                out.putInt(bytesPerFrame);
                out.putInt(bytesPerSample);
            }
        } else if (version == 2) {
            out.putShort((short) 3);
            out.putShort((short) 16);
            out.putShort((short) -2);
            out.putShort((short) 0);
            out.putInt(65536);
            out.putInt(72);
            out.putLong(Double.doubleToLongBits(sampleRate));
            out.putInt(channelCount);
            out.putInt(0x7F000000);
            out.putInt(sampleSize);
            out.putInt(lpcmFlags);
            out.putInt(bytesPerFrame);
            out.putInt(samplesPerPkt);

        }
        writeExtensions(out);
    }

    public short getChannelCount() {
        return channelCount;
    }

    public int calcFrameSize() {
        if (version == 0 || bytesPerFrame == 0)
            return (sampleSize >> 3) * channelCount;
        else
            return bytesPerFrame;
    }

    public int calcSampleSize() {
        return calcFrameSize() / channelCount;
    }

    public short getSampleSize() {
        return sampleSize;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getBytesPerFrame() {
        return bytesPerFrame;
    }

    public int getBytesPerSample() {
        return bytesPerSample;
    }

    public short getVersion() {
        return version;
    }

    public static class MyFactory extends BoxFactory {
        private final Map<String, Class<? extends Box>> mappings;

        public MyFactory() {
            this.mappings = new HashMap<String, Class<? extends Box>>();
            mappings.put(WaveExtension.fourcc(), WaveExtension.class);
            mappings.put(ChannelBox.fourcc(), ChannelBox.class);
            mappings.put("esds", LeafBox.class);
        }

        public Class<? extends Box> toClass(String fourcc) {
            return mappings.get(fourcc);
        }
    }

    public Endian getEndian() {
        EndianBox endianBox = Box.findFirst(this, EndianBox.class, WaveExtension.fourcc(), EndianBox.fourcc());
        if (endianBox == null) {
            if ("twos".equals(header.getFourcc()))
                return Endian.BIG_ENDIAN;
            else if ("lpcm".equals(header.getFourcc()))
                return (lpcmFlags & kAudioFormatFlagIsBigEndian) != 0 ? Endian.BIG_ENDIAN : Endian.LITTLE_ENDIAN;
            else if ("sowt".equals(header.getFourcc()))
                return Endian.LITTLE_ENDIAN;
            else
                return Endian.BIG_ENDIAN;
        }
        return endianBox.getEndian();
    }

    public boolean isFloat() {
        return "fl32".equals(header.getFourcc()) || "fl64".equals(header.getFourcc())
                || ("lpcm".equals(header.getFourcc()) && (lpcmFlags & kAudioFormatFlagIsFloat) != 0);
    }

    public static Set<String> pcms = new HashSet<String>();

    static {
        pcms.add("raw ");
        pcms.add("twos");
        pcms.add("sowt");
        pcms.add("fl32");
        pcms.add("fl64");
        pcms.add("in24");
        pcms.add("in32");
        pcms.add("lpcm");
    }

    public boolean isPCM() {
        return pcms.contains(header.getFourcc());
    }

    public AudioFormat getFormat() {
        return new AudioFormat((int) sampleRate, calcSampleSize() << 3, channelCount, true,
                getEndian() == Endian.BIG_ENDIAN);
    }

    public ChannelLabel[] getLabels() {
        ChannelBox channelBox = Box.findFirst(this, ChannelBox.class, "chan");
        if (channelBox != null) {
            Label[] labels = ChannelUtils.getLabels(channelBox);
            if (channelCount == 2)
                return translate(translationStereo, labels);
            else
                return translate(translationSurround, labels);
        } else {
            switch (channelCount) {
            case 1:
                return new ChannelLabel[] { ChannelLabel.MONO };
            case 2:
                return new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT };
            case 6:
                return new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                        ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT };
            default:
                ChannelLabel[] lbl = new ChannelLabel[channelCount];
                Arrays.fill(lbl, ChannelLabel.MONO);
                return lbl;
            }
        }
    }

    private ChannelLabel[] translate(Map<Label, ChannelLabel> translation, Label[] labels) {
        ChannelLabel[] result = new ChannelLabel[labels.length];
        int i = 0;
        for (Label label : labels) {
            result[i++] = translation.get(label);
        }
        return result;
    }

    private static Map<Label, ChannelLabel> translationStereo = new HashMap<Label, ChannelLabel>();
    private static Map<Label, ChannelLabel> translationSurround = new HashMap<Label, ChannelLabel>();

    static {
        translationStereo.put(Label.Left, ChannelLabel.STEREO_LEFT);
        translationStereo.put(Label.Right, ChannelLabel.STEREO_RIGHT);
        translationStereo.put(Label.HeadphonesLeft, ChannelLabel.STEREO_LEFT);
        translationStereo.put(Label.HeadphonesRight, ChannelLabel.STEREO_RIGHT);
        translationStereo.put(Label.LeftTotal, ChannelLabel.STEREO_LEFT);
        translationStereo.put(Label.RightTotal, ChannelLabel.STEREO_RIGHT);
        translationStereo.put(Label.LeftWide, ChannelLabel.STEREO_LEFT);
        translationStereo.put(Label.RightWide, ChannelLabel.STEREO_RIGHT);

        translationSurround.put(Label.Left, ChannelLabel.FRONT_LEFT);
        translationSurround.put(Label.Right, ChannelLabel.FRONT_RIGHT);
        translationSurround.put(Label.LeftCenter, ChannelLabel.FRONT_CENTER_LEFT);
        translationSurround.put(Label.RightCenter, ChannelLabel.FRONT_CENTER_RIGHT);
        translationSurround.put(Label.Center, ChannelLabel.CENTER);
        translationSurround.put(Label.CenterSurround, ChannelLabel.REAR_CENTER);
        translationSurround.put(Label.CenterSurroundDirect, ChannelLabel.REAR_CENTER);
        translationSurround.put(Label.LeftSurround, ChannelLabel.REAR_LEFT);
        translationSurround.put(Label.LeftSurroundDirect, ChannelLabel.REAR_LEFT);
        translationSurround.put(Label.RightSurround, ChannelLabel.REAR_RIGHT);
        translationSurround.put(Label.RightSurroundDirect, ChannelLabel.REAR_RIGHT);
        translationSurround.put(Label.RearSurroundLeft, ChannelLabel.SIDE_LEFT);
        translationSurround.put(Label.RearSurroundRight, ChannelLabel.SIDE_RIGHT);
        translationSurround.put(Label.LFE2, ChannelLabel.LFE);
        translationSurround.put(Label.LFEScreen, ChannelLabel.LFE);
        translationSurround.put(Label.LeftTotal, ChannelLabel.STEREO_LEFT);
        translationSurround.put(Label.RightTotal, ChannelLabel.STEREO_RIGHT);
        translationSurround.put(Label.LeftWide, ChannelLabel.STEREO_LEFT);
        translationSurround.put(Label.RightWide, ChannelLabel.STEREO_RIGHT);
    }

    protected void getModelFields(List<String> list) {
        ToJSON.allFieldsExcept(this.getClass(), "endian", "float", "format", "labels");
    }
}