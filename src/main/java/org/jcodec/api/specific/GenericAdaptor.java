package org.jcodec.api.specific;

import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;

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
    public Picture decodeFrame(Packet packet, int[][] data) {
        return decoder.decodeFrame(packet.getData(), data);
    }

    @Override
    public boolean canSeek(Packet data) {
        return true;
    }

    @Override
    public int[][] allocatePicture() {
        return Picture.create(1920, 1088, ColorSpace.YUV444).getData();
    }
}
