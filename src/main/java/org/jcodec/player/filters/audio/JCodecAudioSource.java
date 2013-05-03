package org.jcodec.player.filters.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.AudioDecoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.tools.Debug;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.PacketSource;

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
    private List<ByteBuffer> drain = new ArrayList<ByteBuffer>();

    public JCodecAudioSource(PacketSource pkt) throws IOException {
        Debug.println("Creating audio source");

        this.pkt = pkt;
        mediaInfo = (MediaInfo.AudioInfo) pkt.getMediaInfo();
        decoder = JCodecUtil.getAudioDecoder(mediaInfo.getFourcc(), mediaInfo);

        Debug.println("Created audio source");
    }

    @Override
    public AudioFrame getFrame(ByteBuffer out) throws IOException {
        ByteBuffer buffer;
        synchronized (drain) {
            if (drain.size() == 0) {
                drain.add(allocateBuffer());
            }
            buffer = drain.remove(0);
        }
        buffer.rewind();
        Packet packet = pkt.getPacket(buffer);
        if (packet == null)
            return null;
        return new AudioFrame(decoder.decodeFrame(packet.getData(), out), packet.getPts(), packet.getDuration(),
                packet.getTimescale(), (int) packet.getFrameNo());
    }

    private ByteBuffer allocateBuffer() {
        return ByteBuffer.allocate(mediaInfo.getFramesPerPacket() * mediaInfo.getFormat().getFrameSize() * 10);
    }

    public boolean drySeek(RationalLarge second) throws IOException {
        return pkt.drySeek(second);
    }

    public void seek(RationalLarge second) throws IOException {
        pkt.seek(second);
    }

    @Override
    public MediaInfo.AudioInfo getAudioInfo() throws IOException {
        return (MediaInfo.AudioInfo) pkt.getMediaInfo();
    }

    public void close() throws IOException {
        pkt.close();
    }
}