package org.jcodec.containers.mp4.boxes;

import static org.jcodec.containers.mp4.boxes.channel.ChannelLayout.kCAFChannelLayoutTag_UseChannelBitmap;
import static org.jcodec.containers.mp4.boxes.channel.ChannelLayout.kCAFChannelLayoutTag_UseChannelDescriptions;

import org.jcodec.api.NotSupportedException;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.Label;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.mp4.boxes.ChannelBox.ChannelDescription;
import org.jcodec.containers.mp4.boxes.channel.ChannelLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static Map<Codec, String> codec2fourcc = new HashMap<Codec, String>();

    static {
        codec2fourcc.put(Codec.H264, "avc1");
        codec2fourcc.put(Codec.AAC, "mp4a");
        codec2fourcc.put(Codec.PRORES, "apch");
        codec2fourcc.put(Codec.JPEG, "mjpg");
        codec2fourcc.put(Codec.PNG, "png ");
        codec2fourcc.put(Codec.V210, "v210");
    }

    //@formatter:off
    public static int kAudioFormatFlagIsFloat = 0x1;
    public static int kAudioFormatFlagIsBigEndian = 0x2;
    public static int kAudioFormatFlagIsSignedInteger = 0x4;
    public static int kAudioFormatFlagIsPacked = 0x8;
    public static int kAudioFormatFlagIsAlignedHigh = 0x10;
    public static int kAudioFormatFlagIsNonInterleaved = 0x20;
    public static int kAudioFormatFlagIsNonMixable = 0x40;
    //@formatter:on    

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
    private static final List<Label> MONO = Arrays.asList(Label.Mono);
    private static final List<Label> STEREO = Arrays.asList(Label.Left, Label.Right);
    private static final List<Label> MATRIX_STEREO = Arrays.asList(Label.LeftTotal, Label.RightTotal);
    public static final Label[] EMPTY = new Label[0];

    public AudioSampleEntry(Header atom) {
        super(atom);
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

    public ByteOrder getEndian() {
        EndianBox endianBox = NodeBox.findFirstPath(this, EndianBox.class, new String[] { WaveExtension.fourcc(), EndianBox.fourcc() });
        if (endianBox == null) {
            if ("twos".equals(header.getFourcc()))
                return ByteOrder.BIG_ENDIAN;
            else if ("lpcm".equals(header.getFourcc()))
                return (lpcmFlags & kAudioFormatFlagIsBigEndian) != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            else if ("sowt".equals(header.getFourcc()))
                return ByteOrder.LITTLE_ENDIAN;
            else
                return ByteOrder.BIG_ENDIAN;
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
                getEndian() == ByteOrder.BIG_ENDIAN);
    }

    public ChannelLabel[] getLabels() {
        ChannelBox channelBox = NodeBox.findFirst(this, ChannelBox.class, "chan");
        if (channelBox != null) {
            Label[] labels = AudioSampleEntry.getLabelsFromChan(channelBox);
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
        for (int j = 0; j < labels.length; j++) {
            Label label = labels[j];
            result[i++] = translation.get(label);
        }
        return result;
    }

    public static AudioSampleEntry compressedAudioSampleEntry(String fourcc, int drefId, int sampleSize, int channels,
            int sampleRate, int samplesPerPacket, int bytesPerPacket, int bytesPerFrame) {
        AudioSampleEntry ase = createAudioSampleEntry(Header.createHeader(fourcc, 0), (short) drefId,
                (short) channels, (short) 16, sampleRate, (short) 0, 0, 65534, 0, samplesPerPacket, bytesPerPacket,
                bytesPerFrame, 16 / 8, (short) 0);
        return ase;
    }

    public static AudioSampleEntry audioSampleEntry(String fourcc, int drefId, int sampleSize, int channels,
            int sampleRate, ByteOrder endian) {
        AudioSampleEntry ase = createAudioSampleEntry(Header.createHeader(fourcc, 0), (short) drefId,
                (short) channels, (short) 16, sampleRate, (short) 0, 0, 65535, 0, 1, sampleSize, channels * sampleSize,
                sampleSize, (short) 1);
    
        NodeBox wave = new NodeBox(new Header("wave"));
        ase.add(wave);
    
        wave.add(FormatBox.createFormatBox(fourcc));
        wave.add(EndianBox.createEndianBox(endian));
        wave.add(Box.terminatorAtom());
        // ase.add(new ChannelBox(atom));
    
        return ase;
    }

    public static String lookupFourcc(AudioFormat format) {
        if (format.getSampleSizeInBits() == 16 && !format.isBigEndian())
            return "sowt";
        else if (format.getSampleSizeInBits() == 24)
            return "in24";
        else
            throw new NotSupportedException("Audio format " + format + " is not supported.");
    }

    public static AudioSampleEntry audioSampleEntryPCM(AudioFormat format) {
        return audioSampleEntry(AudioSampleEntry.lookupFourcc(format), 1, format.getSampleSizeInBits() >> 3,
                format.getChannels(), (int) format.getSampleRate(),
                format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    }
    
    public static AudioSampleEntry audioSampleEntry(AudioCodecMeta meta) {
        AudioFormat format = meta.getFormat();
        AudioSampleEntry ase = AudioSampleEntry.compressedAudioSampleEntry(codec2fourcc.get(meta.getCodec()), (short) 1, (short) 16,
                format.getChannels(), format.getSampleRate(), 0, 0, 0);
        return ase;
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

    public static Label[] getLabelsFromSampleEntry(AudioSampleEntry se) {
        ChannelBox channel = NodeBox.findFirst(se, ChannelBox.class, "chan");
        if (channel != null)
            return getLabelsFromChan(channel);
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

    public static Label[] getLabelsFromTrack(TrakBox trakBox) {
        return AudioSampleEntry.getLabelsFromSampleEntry((AudioSampleEntry) trakBox.getSampleEntries()[0]);
    }

    public static void setLabel(TrakBox trakBox, int channel, Label label) {
        Label[] labels = AudioSampleEntry.getLabelsFromTrack(trakBox);
        labels[channel] = label;
        _setLabels(trakBox, labels);
    }

    public static void _setLabels(TrakBox trakBox, Label[] labels) {
        ChannelBox channel = NodeBox.findFirstPath(trakBox, ChannelBox.class, new String[] { "mdia", "minf", "stbl", "stsd", null, "chan" });
        if (channel == null) {
            channel = ChannelBox.createChannelBox();
            NodeBox.findFirstPath(trakBox, SampleEntry.class, new String[] { "mdia", "minf", "stbl", "stsd", null }).add(channel);
        }
        setLabels(labels, channel);
    }

    public static void setLabels(Label[] labels, ChannelBox channel) {
        channel.setChannelLayout(kCAFChannelLayoutTag_UseChannelDescriptions.getCode());
        ChannelDescription[] list = new ChannelDescription[labels.length];
        for (int i = 0; i < labels.length; i++)
            list[i] = new ChannelBox.ChannelDescription(labels[i].getVal(), 0, new float[] { 0, 0, 0 });
        channel.setDescriptions(list);
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
        Label[] values = Label.values();
        for (int i = 0; i < values.length; i++) {
            Label label = values[i];
            if ((label.bitmapVal & channelBitmap) != 0)
                result.add(label);
        }
        return result.toArray(new Label[0]);
    }

    public static Label[] extractLabels(ChannelDescription[] descriptions) {
        Label[] result = new Label[descriptions.length];
        for (int i = 0; i < descriptions.length; i++)
            result[i] = descriptions[i].getLabel();
        return result;
    }

    public static Label[] getLabelsFromChan(ChannelBox box) {
        long tag = box.getChannelLayout();
        if ((tag >> 16) == 147) {
            int n = (int) tag & 0xffff;
            Label[] res = new Label[n];
            for (int i = 0; i < n; i++)
                res[i] = Label.getByVal((1 << 16) | i);
            return res;
        }
        ChannelLayout[] values = ChannelLayout.values();
        for (int i = 0; i < values.length; i++) {
            ChannelLayout layout = values[i];
            if (layout.getCode() == tag) {
                if (layout == kCAFChannelLayoutTag_UseChannelDescriptions) {
                    return extractLabels(box.getDescriptions());
                } else if (layout == kCAFChannelLayoutTag_UseChannelBitmap) {
                    return getLabelsByBitmap(box.getChannelBitmap());
                } else {
                    return layout.getLabels();
                }
            }
        }
        return AudioSampleEntry.EMPTY;
    }
    
    
}