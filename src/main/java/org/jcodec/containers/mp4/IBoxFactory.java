package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;

public interface IBoxFactory {

    Class<? extends Box> toClass(String fourcc);

    Box newBox(Header header);
}