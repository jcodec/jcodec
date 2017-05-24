package org.jcodec.api.transcode;

import java.io.IOException;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.model.Packet;


/**
 * The sink that consumes the uncompressed frames and stores them into a
 * compressed file.
 * 
 * @author Stanislav Vitvitskiy
 */
public interface PacketSink {

    void outputVideoPacket(Packet videoPacket, VideoCodecMeta videoCodecMeta) throws IOException;

    void outputAudioPacket(Packet audioPacket, AudioCodecMeta audioCodecMeta) throws IOException;
   
}
