package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.Box;

import js.util.HashMap;
import js.util.Map;

public abstract class Boxes {
    protected final Map<String, Class<? extends Box>> mappings;

    public Boxes() {
        this.mappings = new HashMap<String, Class<? extends Box>>();
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }

    public void override(String fourcc, Class<? extends Box> cls) {
        mappings.put(fourcc, cls);
    }

    public void clear() {
        mappings.clear();
    }

}
