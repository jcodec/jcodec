package org.jcodec.codecs.aac;

import static org.jcodec.codecs.aac.ObjectType.AOT_ESCAPE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.DemuxerTrack;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AACUtils {

    public static class AudioInfo {
        private AudioFormat format;
        private ChannelLabel[] labels;

        public AudioInfo(AudioFormat format, ChannelLabel[] labels) {
            this.format = format;
            this.labels = labels;
        }

        public AudioFormat getFormat() {
            return format;
        }

        public ChannelLabel[] getLabels() {
            return labels;
        }
    }

    private static int getObjectType(InBits reader) throws IOException {
        int objectType = reader.readNBit(5);
        if (objectType == AOT_ESCAPE.ordinal())
            objectType = 32 + reader.readNBit(6);
        return objectType;
    }

    private static ChannelLabel[][] AAC_DEFAULT_CONFIGS = {
            null, //
            { ChannelLabel.MONO }, //
            { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT }, //
            { ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT }, //
            { ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_CENTER }, //
            { ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
                    ChannelLabel.REAR_RIGHT }, //
            { ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
                    ChannelLabel.REAR_RIGHT, ChannelLabel.LFE }, //
            { ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.SIDE_LEFT,
                    ChannelLabel.SIDE_RIGHT, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT, ChannelLabel.LFE } //
    };

    public static AudioInfo parseAudioInfo(byte[] privData) throws IOException {
        InBits reader = new BitstreamReader(new ByteArrayInputStream(privData));

        int objectType = getObjectType(reader);
        int index = reader.readNBit(4);
        int sampleRate = index == 0x0f ? reader.readNBit(24) : AACConsts.AAC_SAMPLE_RATES[index];
        int channelConfig = reader.readNBit(4);

        if (channelConfig == 0 || channelConfig >= AAC_DEFAULT_CONFIGS.length)
            return null;

        ChannelLabel[] channels = AAC_DEFAULT_CONFIGS[channelConfig];
        return new AudioInfo(new AudioFormat(sampleRate, 16, channels.length, true, false), channels);
    }

    public static AudioInfo getChannels(SampleEntry mp4a) throws IOException {
        if (!"mp4a".equals(mp4a.getFourcc()))
            throw new IllegalArgumentException("Not mp4a sample entry");
        LeafBox b = Box.findFirst(mp4a, LeafBox.class, "esds");
        if (b == null)
            return null;
        EsdsBox esds = new EsdsBox();
        esds.parse(new ByteArrayInputStream(b.getData()));
        return parseAudioInfo(esds.getStreamInfo());
    }
}