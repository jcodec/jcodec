package org.jcodec.player.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.AudioDecoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.Debug;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecAudioSource implements AudioSource {

    private PacketSource pkt;
    private AudioDecoder decoder;
    private MediaInfo.AudioInfo mediaInfo;
    private List<byte[]> drain = new ArrayList<byte[]>();

    public JCodecAudioSource(PacketSource pkt) {
        Debug.println("Creating audio source");

        this.pkt = pkt;
        mediaInfo = (MediaInfo.AudioInfo) pkt.getMediaInfo();
        decoder = JCodecUtil.getAudioDecoder(mediaInfo.getFourcc(), mediaInfo);

        Debug.println("Created audio source");
    }

    @Override
    public AudioFrame getFrame(byte[] out) throws IOException {
        byte[] buffer;
        synchronized (drain) {
            if (drain.size() == 0) {
                drain.add(allocateBuffer());
            }
            buffer = drain.remove(0);
        }
        Packet packet = pkt.getPacket(buffer);
        if (packet == null)
            return null;
        return new AudioFrame(decoder.decodeFrame(packet.getData(), out), packet.getPts(), packet.getDuration(),
                packet.getTimescale());
    }

    private byte[] allocateBuffer() {
        return new byte[mediaInfo.getFramesPerPacket() * mediaInfo.getFormat().getFrameSize() * 10];
    }

    public boolean seek(long clock, long timescale) throws IOException {
        return pkt.seek(clock * mediaInfo.getTimescale() / timescale);
    }

    @Override
    public MediaInfo.AudioInfo getAudioInfo() {
        return (MediaInfo.AudioInfo) pkt.getMediaInfo();
    }

    public void close() {
    }
}