package org.jcodec.codecs.h264.io.read.rtp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.codecs.h264.annexb.AnnexBDemuxer.NALUnitSource;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads NAL units from RTP stream
 * 
 * 
 * @author Jay Codec
 * 
 */
public class RTPNUReader implements NALUnitSource {

    private PacketSource src;

    public RTPNUReader(PacketSource src) {
        this.src = src;
    }

    public InputStream nextNALUnit() throws IOException {
        Packet packet = src.getNextPacket();
        if (packet == null)
            return null;
        byte[] payload = packet.getPayload();

        ByteArrayInputStream stream = new ByteArrayInputStream(payload);

        return stream;
    }

}
