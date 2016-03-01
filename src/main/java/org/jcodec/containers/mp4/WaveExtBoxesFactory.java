package org.jcodec.containers.mp4;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.EndianBox;
import org.jcodec.containers.mp4.boxes.FormatBox;

public class WaveExtBoxesFactory extends BoxFactory {
    private Map<String, Class<? extends Box>> mappings;

    public WaveExtBoxesFactory() {
        this.mappings = new HashMap<String, Class<? extends Box>>();

        mappings.put(FormatBox.fourcc(), FormatBox.class);
        mappings.put(EndianBox.fourcc(), EndianBox.class);
        //            mappings.put(EsdsBox.fourcc(), EsdsBox.class);
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }
}