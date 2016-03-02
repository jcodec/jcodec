package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.EndianBox;
import org.jcodec.containers.mp4.boxes.FormatBox;

public class WaveExtBoxes extends Boxes {
    public WaveExtBoxes() {
        mappings.put(FormatBox.fourcc(), FormatBox.class);
        mappings.put(EndianBox.fourcc(), EndianBox.class);
        //            mappings.put(EsdsBox.fourcc(), EsdsBox.class);
    }
}