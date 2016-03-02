package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.AliasBox;
import org.jcodec.containers.mp4.boxes.UrlBox;

public class DataBoxes extends Boxes {
    public DataBoxes() {
        mappings.put(UrlBox.fourcc(), UrlBox.class);
        mappings.put(AliasBox.fourcc(), AliasBox.class);
        mappings.put("cios", AliasBox.class);
    }
}