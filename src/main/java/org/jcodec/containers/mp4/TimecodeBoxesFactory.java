package org.jcodec.containers.mp4;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.containers.mp4.boxes.Box;

public class TimecodeBoxesFactory extends BoxFactory {
    private Map<String, Class<? extends Box>> mappings;

    public TimecodeBoxesFactory() {
        this.mappings = new HashMap<String, Class<? extends Box>>();
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }
}