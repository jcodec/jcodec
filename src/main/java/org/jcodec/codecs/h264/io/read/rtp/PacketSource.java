package org.jcodec.codecs.h264.io.read.rtp;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A source of incomint RTP packets
 * 
 * @author Jay Codec
 * 
 */
public interface PacketSource {

    Packet getNextPacket() throws IOException;

}
