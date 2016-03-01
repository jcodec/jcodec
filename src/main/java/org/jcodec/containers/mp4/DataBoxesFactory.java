package org.jcodec.containers.mp4;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.containers.mp4.boxes.AliasBox;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.UrlBox;

public class DataBoxesFactory extends BoxFactory {
    private final Map<String, Class<? extends Box>> mappings;

    public DataBoxesFactory() {
        this.mappings = new HashMap<String, Class<? extends Box>>();
        mappings.put(UrlBox.fourcc(), UrlBox.class);
        mappings.put(AliasBox.fourcc(), AliasBox.class);
        mappings.put("cios", AliasBox.class);
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }
}