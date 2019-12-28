package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.BitRateBox;
import org.jcodec.containers.mp4.boxes.TextConfigBox;
import org.jcodec.containers.mp4.boxes.URIBox;
import org.jcodec.containers.mp4.boxes.URIInitBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MetaDataBoxes extends Boxes {
    public MetaDataBoxes() {
        mappings.put(URIBox.fourcc(), URIBox.class);
        mappings.put(URIInitBox.fourcc(), URIInitBox.class);
        mappings.put(BitRateBox.fourcc(), BitRateBox.class);
        mappings.put(TextConfigBox.fourcc(), TextConfigBox.class);
    }
}
