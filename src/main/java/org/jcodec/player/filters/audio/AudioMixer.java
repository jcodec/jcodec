package org.jcodec.player.filters.audio;

import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.filters.MediaInfo.AudioInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Mixes incoming audio tracks and produces 16bit stereo or 5.1 output big
 * endian
 * 
 * @author The JCodec project
 * 
 */
public class AudioMixer implements AudioSource {

    public static final int NUM_FRAMES = 2048;

    private int sampleRate;
    private Pin[] pins;
    private int dstChannels;
    private AudioFormat dstFormat;
    private long curFrame;
    private FloatFrame[] nextFrame;

    private class FloatFrame {
        long startFrame;
        int frames;
        int pattern;
        ChannelLabel[] labels;
        float[] data;
        int framePos;

        public FloatFrame(long startFrame, int frames, int pattern, ChannelLabel[] labels, float[] data) {
            this.startFrame = startFrame;
            this.frames = frames;
            this.pattern = pattern;
            this.labels = labels;
            this.data = data;
        }
    }

    public class Pin {
        private int pattern;
        private AudioSource src;
        private float[] floatBuf;
        private byte[] byteBuf;
        public final static float r16 = 1f / 32768f;
        public final static float r24 = 1f / 8388608f;
        private int channels;
        private AudioInfo audioInfo;

        private Pin(AudioSource src) throws IOException {
            this.src = src;
            this.audioInfo = src.getAudioInfo();
            this.channels = audioInfo.getFormat().getChannels();
            this.pattern = (1 << channels) - 1;
            this.byteBuf = new byte[audioInfo.getFormat().getFrameSize() * audioInfo.getFramesPerPacket() * 2];
            this.floatBuf = new float[NUM_FRAMES * channels];
        }

        public void mute(int channel) {
            if (channel >= audioInfo.getFormat().getChannels())
                throw new IllegalArgumentException("Invalid channel " + channel);
            pattern &= ~(1 << channel);
        }

        public void unmute(int channel) {
            if (channel >= audioInfo.getFormat().getChannels())
                throw new IllegalArgumentException("Invalid channel " + channel);
            pattern |= 1 << channel;
        }

        public void toggle(int channel) {
            if (channel >= audioInfo.getFormat().getChannels())
                throw new IllegalArgumentException("Invalid channel " + channel);
            pattern ^= 1 << channel;
        }

        public FloatFrame getFrame() throws IOException {
            AudioFrame frame = src.getFrame(byteBuf);
            if (frame == null)
                return null;
            int samples = toFloat(frame.getData(), floatBuf);
            return new FloatFrame((frame.getPts() * sampleRate) / frame.getTimescale(), samples / channels, pattern,
                    audioInfo.getLabels(), floatBuf);
        }

        private int toFloat(Buffer buf, float[] floatBuf) {
            if (audioInfo.getFormat().isBigEndian()) {
                if (audioInfo.getFormat().getSampleSizeInBits() == 16) {
                    return toFloat16BE(buf, floatBuf);
                } else {
                    return toFloat24BE(buf, floatBuf);
                }
            } else {
                if (audioInfo.getFormat().getSampleSizeInBits() == 16) {
                    return toFloat16LE(buf, floatBuf);
                } else {
                    return toFloat24LE(buf, floatBuf);
                }
            }
        }

        private int toFloat24LE(Buffer buf, float[] out) {
            int samples = 0;
            while (buf.limit - buf.pos >= 3 && samples < out.length) {
                out[samples++] = r24
                        * ((((buf.buffer[buf.pos++] & 0xff) << 8) | ((buf.buffer[buf.pos++] & 0xff) << 16) | ((buf.buffer[buf.pos++] & 0xff) << 24)) >> 8);
            }
            return samples;
        }

        private int toFloat16LE(Buffer buf, float[] out) {
            int samples = 0;
            while (buf.limit - buf.pos >= 2 && samples < out.length) {
                out[samples++] = r16 * (short) ((buf.buffer[buf.pos++] & 0xff) | ((buf.buffer[buf.pos++] & 0xff) << 8));
            }
            return samples;
        }

        private int toFloat24BE(Buffer buf, float[] out) {
            int samples = 0;
            while (buf.limit - buf.pos >= 3 && samples < out.length) {
                out[samples++] = r24
                        * ((((buf.buffer[buf.pos++] & 0xff) << 24) | ((buf.buffer[buf.pos++] & 0xff) << 16) | ((buf.buffer[buf.pos++] & 0xff) << 8)) >> 8);
            }
            return samples;
        }

        private int toFloat16BE(Buffer buf, float[] out) {
            int samples = 0;
            while (buf.limit - buf.pos >= 2 && samples < out.length) {
                out[samples++] = r16 * (short) (((buf.buffer[buf.pos++] & 0xff) << 8) | (buf.buffer[buf.pos++] & 0xff));
            }
            return samples;
        }

        public void close() throws IOException {
            src.close();
        }

        public ChannelLabel[] getLabels() {
            return audioInfo.getLabels();
        }

        public AudioSource getSource() {
            return src;
        }

        public int[] getSoloChannels() {
            TIntArrayList result = new TIntArrayList();
            for (int i = 0; i < 32; i++)
                if (((pattern >> i) & 0x1) == 1)
                    result.add(i);
            return result.toArray();
        }

        public int getSampleRate() {
            return (int) audioInfo.getFormat().getSampleRate();
        }
    }

    public AudioMixer(int channels, AudioSource... src) throws IOException {
        if(src.length < 1)
            throw new IllegalArgumentException("Must be at least one audio source");
        pins = new Pin[src.length];
        this.dstChannels = channels;
        for (int i = 0; i < src.length; i++) {
            pins[i] = new Pin(src[i]);
        }
        this.sampleRate = pins[0].getSampleRate();
        for (int i = 0; i < pins.length; i++) {
            if (pins[i].getSampleRate() != sampleRate)
                throw new IllegalArgumentException("Sample rate conversion is not supported. Remove " + i
                        + "th audio source (" + src[i].getAudioInfo().getFormat() + ")");
        }

        dstFormat = new AudioFormat(sampleRate, 16, channels, true, false);
    }

    @Override
    public AudioInfo getAudioInfo() throws IOException {
        long maxDuration = Long.MIN_VALUE, maxFrames = Long.MIN_VALUE;
        for (Pin pin : pins) {
            AudioInfo ai = pin.getSource().getAudioInfo();
            long duration = (ai.getDuration() * sampleRate) / ai.getTimescale();
            if (duration > maxDuration)
                maxDuration = duration;
            if (ai.getNFrames() > maxFrames)
                maxFrames = ai.getNFrames();
        }

        return new AudioInfo("sowt", sampleRate, maxDuration, maxFrames, "", dstFormat, NUM_FRAMES,
                dstChannels == 2 ? new ChannelLabel[] { ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT }
                        : new ChannelLabel[] { ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                                ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT });
    }

    @Override
    public synchronized AudioFrame getFrame(byte[] buf) throws IOException {
        Buffer out = new Buffer(buf);

        if (nextFrame == null) {
            nextFrame = new FloatFrame[pins.length];
            for (int i = 0; i < pins.length; i++)
                nextFrame[i] = pins[i].getFrame();
        }

        long minFrame = Long.MAX_VALUE;
        for (int i = 0; i < nextFrame.length; i++) {
            if (nextFrame[i] != null && nextFrame[i].startFrame < minFrame)
                minFrame = nextFrame[i].startFrame;
        }
        if (minFrame == Long.MAX_VALUE)
            return null;
        else if (minFrame > curFrame)
            curFrame = minFrame;

        long startFrame = curFrame;

        for (int leftPkt = NUM_FRAMES; leftPkt > 0;) {
            int mixedFrames = mix(out, nextFrame, curFrame, leftPkt);
            curFrame += mixedFrames;
            leftPkt -= mixedFrames;

            for (int i = 0; i < nextFrame.length; i++) {
                if (nextFrame[i] == null || nextFrame[i].startFrame + nextFrame[i].frames <= curFrame)
                    nextFrame[i] = pins[i].getFrame();
            }
        }

        return new AudioFrame(new AudioBuffer(out.flip(), dstFormat, NUM_FRAMES), startFrame, NUM_FRAMES, sampleRate,
                (int) (startFrame / NUM_FRAMES));
    }

    static float[][] contributions = new float[][] { new float[] { 1, .7f, .7f, .7f, .7f, 1, 0, .7f, .7f },
            new float[] { 1, 1, 0, .7f, 0, 1, 0, .7f, 0 }, new float[] { 1, 0, 1, 0, .7f, 1, 0, 0, .7f },
            new float[] { 1, 1, 0, 1, 0, 0, 0, 0, 0 }, new float[] { 1, 0, 1, 0, 1, 0, 0, 0, 0 },
            new float[] { 1, 1, 1, 0, 0, 1, 0, 0, 0 }, new float[] { 1, 1, 1, 0, 0, 0, 1, 0, 0 },
            new float[] { .7f, .7f, 0, 0, 0, 0, 0, 1, 0 }, new float[] { .7f, 0, .7f, 0, 0, 0, 0, 0, 1 },
            new float[] { 1, 1, 0, 0, 0, 1, 0, 0, 0 }, new float[] { 1, 0, 1, 0, 0, 1, 0, 0, 0 },
            new float[] { .7f, 1, 1, 0, 0, 0, 0, .7f, .7f }, new float[] { .7f, .7f, 0, 0, 0, 0, 0, 1, 0 },
            new float[] { .7f, 0, .7f, 0, 0, 0, 0, 0, 1 } };

    private int mix(Buffer out, FloatFrame[] in, long curFrame, int maxFrames) throws IOException {

        float[] sum = new float[dstChannels];
        float[] mul = new float[dstChannels];
        Arrays.fill(mul, 1f);
        int[] count = new int[dstChannels];
        int contribOffset = (dstChannels == 1 ? 0 : (dstChannels == 2 ? 1 : 3));

        int frame;
        for (frame = 0; frame < maxFrames; frame++, curFrame++) {
            for (int track = 0; track < in.length; track++) {
                if (in[track] == null || curFrame < in[track].startFrame)
                    continue;
                if (curFrame >= in[track].startFrame + in[track].frames)
                    return frame;
                FloatFrame floatFrame = in[track];
                int channels = floatFrame.labels.length;
                for (int channel = 0; channel < channels; channel++) {
                    float sample = floatFrame.data[floatFrame.framePos++];
                    if (((in[track].pattern >> channel) & 0x1) != 1)
                        continue;
                    float[] fs = contributions[in[track].labels[channel].ordinal()];
                    if (fs == null)
                        throw new RuntimeException("Label " + in[track].labels[channel] + " is not supported");

                    for (int i = 0; i < dstChannels; i++) {
                        float gain = fs[i + contribOffset] * sample;
                        sum[i] += gain;
                        mul[i] *= gain;
                        count[i] += fs[i + contribOffset] != 0 ? 1 : 0;
                    }
                }
            }

            for (int i = 0; i < dstChannels; i++) {
                float val = count[i] > 1 ? clamp1f(sum[i] - mul[i]) : sum[i];
                int sample = floatToSigned16Pack(val);
                out.write(sample & 0xff);
                out.write(sample >> 8);
                count[i] = 0;
                sum[i] = 0f;
                mul[i] = 1f;
            }
        }
        return frame;
    }

    static int floatToSigned16Pack(float f) {
        return ((int) (f * 32768f)) & 0xffff;
    }

    public final static float clamp1f(float f) {
        if (f > 1f)
            return 1f;
        if (f < -1f)
            return -1f;
        return f;
    }

    @Override
    public boolean drySeek(RationalLarge second) throws IOException {
        boolean success = true;
        for (Pin pin : pins) {
            success &= pin.getSource().drySeek(second);
        }
        return success;
    }

    @Override
    public synchronized void seek(RationalLarge second) throws IOException {
        for (Pin pin : pins)
            pin.getSource().seek(second);
        nextFrame = null;
        curFrame = 0;
    }

    @Override
    public void close() throws IOException {
        for (Pin pin : pins) {
            pin.close();
        }
    }

    public Pin[] getPins() {
        return pins;
    }
}