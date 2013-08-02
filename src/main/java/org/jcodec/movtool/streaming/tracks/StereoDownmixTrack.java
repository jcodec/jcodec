package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;

import org.jcodec.containers.mp4.boxes.EndianBox.Endian;
import org.jcodec.containers.mp4.boxes.channel.Label;
import org.jcodec.movtool.streaming.AudioCodecMeta;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Downmixes a set of input audio tracks ( possibly with multiple channels ) to
 * one stereo track.
 * 
 * Produces frames of exactly equal size regarless of underlying tracks PCM
 * chunk sizes
 * 
 * @author The JCodec project
 * 
 */
public class StereoDownmixTrack implements VirtualTrack {
    private final static int FRAMES_IN_OUT_PACKET = 1024 * 20;
    private VirtualTrack[] sources;
    private AudioCodecMeta[] sampleEntries;
    private int rate;
    private int frameNo;
    private boolean[][] solo;
    private DownmixHelper downmix;

    public StereoDownmixTrack(VirtualTrack... tracks) {
        this.rate = -1;
        sources = new VirtualTrack[tracks.length];
        sampleEntries = new AudioCodecMeta[sources.length];
        solo = new boolean[tracks.length][];
        for (int i = 0; i < tracks.length; i++) {
            CodecMeta se = tracks[i].getCodecMeta();
            if (!(se instanceof AudioCodecMeta))
                throw new IllegalArgumentException("Non audio track");
            AudioCodecMeta ase = (AudioCodecMeta) se;
            if (!ase.isPCM())
                throw new IllegalArgumentException("Non PCM audio track.");
            AudioFormat format = ase.getFormat();
            if (rate != -1 && rate != format.getFrameRate())
                throw new IllegalArgumentException("Can not downmix tracks of different rate.");
            rate = (int) format.getFrameRate();
            sampleEntries[i] = ase;
            sources[i] = new PCMFlatternTrack(tracks[i], FRAMES_IN_OUT_PACKET);
            solo[i] = new boolean[format.getChannels()];
        }
        
        downmix = new DownmixHelper(sampleEntries, FRAMES_IN_OUT_PACKET, null);
    }

    public void soloTrack(int track, boolean s) {
        for (int ch = 0; ch < solo[track].length; ch++) {
            solo[track][ch] = s;
        }
        downmix = new DownmixHelper(sampleEntries, FRAMES_IN_OUT_PACKET, solo);
    }
    
    public void soloChannel(int track, int channel, boolean s) {
        solo[track][channel] = s;
        downmix = new DownmixHelper(sampleEntries, FRAMES_IN_OUT_PACKET, solo);
    }

    public boolean isChannelMute(int track, int channel) {
        return solo[track][channel];
    }
    
    public boolean[][] bulkGetSolo() {
        return solo;
    }
    
    public void bulkSetSolo(boolean[][] solo) {
        this.solo = solo;
        downmix = new DownmixHelper(sampleEntries, FRAMES_IN_OUT_PACKET, solo);
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        VirtualPacket[] packets = new VirtualPacket[sources.length];
        boolean allNull = true;
        for (int i = 0; i < packets.length; i++) {
            packets[i] = sources[i].nextPacket();
            allNull &= packets[i] == null;
        }
        if (allNull)
            return null;

        VirtualPacket ret = new DownmixVirtualPacket(packets, frameNo);

        frameNo += FRAMES_IN_OUT_PACKET;

        return ret;
    }

    @Override
    public CodecMeta getCodecMeta() {
        return new AudioCodecMeta("sowt", 2, 2, rate, Endian.LITTLE_ENDIAN, true, new Label[] {Label.Left, Label.Right}, null);
    }

    @Override
    public void close() throws IOException {
        for (VirtualTrack virtualTrack : this.sources) {
            virtualTrack.close();
        }
    }

    protected class DownmixVirtualPacket implements VirtualPacket {
        private VirtualPacket[] packets;
        private int frameNo;

        public DownmixVirtualPacket(VirtualPacket[] packets, int pktNo) {
            this.packets = packets;
            this.frameNo = pktNo;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            ByteBuffer[] data = new ByteBuffer[packets.length];
            for (int i = 0; i < data.length; i++)
                data[i] = packets[i] == null ? null : packets[i].getData();
            ByteBuffer out = ByteBuffer.allocate(FRAMES_IN_OUT_PACKET << 2);

            downmix.downmix(data, out);

            return out;
        }

        @Override
        public int getDataLen() {
            return FRAMES_IN_OUT_PACKET << 2;
        }

        @Override
        public double getPts() {
            return (double) frameNo / rate;
        }

        @Override
        public double getDuration() {
            return (double) FRAMES_IN_OUT_PACKET / rate;
        }

        @Override
        public boolean isKeyframe() {
            return true;
        }

        @Override
        public int getFrameNo() {
            return frameNo;
        }
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return rate;
    }
}