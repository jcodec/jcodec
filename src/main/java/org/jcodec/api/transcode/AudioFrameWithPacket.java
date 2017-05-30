package org.jcodec.api.transcode;

import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class AudioFrameWithPacket {
    private AudioBuffer audio;
    private Packet packet;
    

    public AudioFrameWithPacket(AudioBuffer audio, Packet packet) {
        this.audio = audio;
        this.packet = packet;
    }


    public AudioBuffer getAudio() {
        return audio;
    }


    public Packet getPacket() {
        return packet;
    }
}
