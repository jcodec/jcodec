package org.jcodec.api.specific;

import org.jcodec.api.FrameGrab.MediaInfo;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface ContainerAdaptor {

    Picture decodeFrame(Packet packet, int[][] data);

    boolean canSeek(Packet data);
    
    int[][] allocatePicture();

	MediaInfo getMediaInfo();
    
}
