package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.ReaderBE;
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

    public static int kAudioFormatFlagIsFloat = (1 << 0); // 0x1
    public static int kAudioFormatFlagIsBigEndian = (1 << 1); // 0x2
    public static int kAudioFormatFlagIsSignedInteger = (1 << 2); // 0x4
    public static int kAudioFormatFlagIsPacked = (1 << 3); // 0x8
    public static int kAudioFormatFlagIsAlignedHigh = (1 << 4); // 0x10
    public static int kAudioFormatFlagIsNonInterleaved = (1 << 5); // 0x20
    public static int kAudioFormatFlagIsNonMixable = (1 << 6); // 0x40

    private static final MyFactory FACTORY = new MyFactory();
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

    public AudioSampleEntry(Header header, short drefInd, short channelCount, short sampleSize, int sampleRate,
            short revision, int vendor, int compressionId, int pktSize, int samplesPerPkt, int bytesPerPkt,
            int bytesPerFrame, int bytesPerSample, short version) {
        super(header, drefInd);
        this.channelCount = channelCount;
        this.sampleSize = sampleSize;
        this.sampleRate = sampleRate;
        this.revision = revision;
        this.vendor = vendor;
        this.compressionId = compressionId;
        this.pktSize = pktSize;
        this.samplesPerPkt = samplesPerPkt;
        this.bytesPerPkt = bytesPerPkt;
        this.bytesPerFrame = bytesPerFrame;
        this.bytesPerSample = bytesPerSample;
        this.version = version;
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        version = (short) ReaderBE.readInt16(input);
        revision = (short) ReaderBE.readInt16(input);
        vendor = (int) ReaderBE.readInt32(input);

        channelCount = (short) ReaderBE.readInt16(input);
        sampleSize = (short) ReaderBE.readInt16(input);

        compressionId = (int) ReaderBE.readInt16(input);
        pktSize = (int) ReaderBE.readInt16(input);

        sampleRate = (float) ReaderBE.readInt32(input) / 65536f;

        if (version == 1) {
            samplesPerPkt = (int) ReaderBE.readInt32(input);
            bytesPerPkt = (int) ReaderBE.readInt32(input);
            bytesPerFrame = (int) ReaderBE.readInt32(input);
            bytesPerSample = (int) ReaderBE.readInt32(input);

            parseExtensions(input);
        } else if (version == 2) {
            ReaderBE.readInt32(input); /* sizeof struct only */
            sampleRate = (float) Double.longBitsToDouble(ReaderBE.readInt64(input));
            channelCount = (short) ReaderBE.readInt32(input);
            ReaderBE.readInt32(input); /* always 0x7F000000 */
            sampleSize = (short) ReaderBE.readInt32(input);
            lpcmFlags = (int) ReaderBE.readInt32(input);
            bytesPerFrame = (int) ReaderBE.readInt32(input);
            samplesPerPkt = (int) ReaderBE.readInt32(input);

            parseExtensions(input);
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);

        out.writeShort(version);
        out.writeShort(revision);
        out.writeInt(vendor);

        out.writeShort(channelCount);
        if (version == 0)
            out.writeShort(sampleSize);
        else
            out.writeShort(16);

        out.writeShort(compressionId);
        out.writeShort(pktSize);

        out.writeInt((int) Math.round(sampleRate * 65536d));

        if (version == 1) {
            out.writeInt(samplesPerPkt);
            out.writeInt(bytesPerPkt);
            out.writeInt(bytesPerFrame);
            out.writeInt(bytesPerSample);

            writeExtensions(out);
        } else if (version == 2) {
            out.writeInt(36);
            out.writeLong(Double.doubleToLongBits(sampleRate));
            out.writeInt(channelCount);
            out.writeInt(0x7F000000);
            out.writeInt(sampleSize);
            out.writeInt(lpcmFlags);
            out.writeInt(bytesPerFrame);
            out.writeInt(samplesPerPkt);

            writeExtensions(out);
        }
    }

    public short getChannelCount() {
        return channelCount;
    }

    public int calcFrameSize() {
        if (version == 0)
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
        private Map<String, Class<? extends Box>> mappings = new HashMap<String, Class<? extends Box>>();

        public MyFactory() {
            mappings.put(WaveExtension.fourcc(), WaveExtension.class);
            mappings.put(ChannelBox.fourcc(), ChannelBox.class);
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
        return new AudioFormat(sampleRate, calcSampleSize() << 3, channelCount, true, getEndian() == Endian.BIG_ENDIAN);
    }

    @Override
    public void dump(StringBuilder sb) {
        sb.append(header.getFourcc() + ": {\n");
        sb.append("entry: ");
        ToJSON.toJSON(this, sb, "channelCount", "sampleSize", "sampleRat", "revision", "vendor", "compressionId",
                "pktSize", "samplesPerPkt", "bytesPerPkt", "bytesPerFrame", "bytesPerSample", "version", "lpcmFlags");
        sb.append(",\nexts: [\n");
        dumpBoxes(sb);
        sb.append("\n]\n");
        sb.append("}\n");
    }

    public ChannelLabel[] getLabels() {
        ChannelBox channelBox = Box.findFirst(this, ChannelBox.class, "chan");
        if (channelBox != null) {
            List<Label> labels = ChannelUtils.getLabels(channelBox);
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

    private ChannelLabel[] translate(Map<Label, ChannelLabel> translation, List<Label> labels) {
        ChannelLabel[] result = new ChannelLabel[labels.size()];
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
}