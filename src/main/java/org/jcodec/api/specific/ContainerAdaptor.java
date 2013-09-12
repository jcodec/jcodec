package org.jcodec.api.specific;

import org.jcodec.api.FrameGrab.MediaInfo;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;

public interface ContainerAdaptor {

    Picture decodeFrame(Packet packet, int[][] data);

    boolean canSeek(Packet data);
    
    int[][] allocatePicture();

	MediaInfo getMediaInfo();
    
}
