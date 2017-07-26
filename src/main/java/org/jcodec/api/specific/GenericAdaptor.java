package org.jcodec.api.specific;

import org.jcodec.api.MediaInfo;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * High level frame grabber helper.
 * 
 * @author The JCodec project
 * 
 */
public class GenericAdaptor implements ContainerAdaptor {

    private VideoDecoder decoder;

    public GenericAdaptor(VideoDecoder detect) {
        this.decoder = detect;
    }

    @Override
    public Picture8Bit decodeFrame8Bit(Packet packet, byte[][] data) {
        return decoder.decodeFrame8Bit(packet.getData(), data);
    }

    @Override
    public boolean canSeek(Packet data) {
        return true;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return new MediaInfo(new Size(0, 0));
    }

    @Override
    public byte[][] allocatePicture8Bit() {
        return Picture8Bit.create(1920, 1088, ColorSpace.YUV444).getData();
    }
}
