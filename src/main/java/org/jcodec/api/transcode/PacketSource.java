package org.jcodec.api.transcode;

import java.io.IOException;

import org.jcodec.common.model.Packet;


/**
 * A source for compressed video/audio frames.
 * 
 * @author Stanislav Vitvitskiy
 */
public interface PacketSource {

    Packet inputVideoPacket() throws IOException;

    Packet inputAudioPacket() throws IOException;
    
}
