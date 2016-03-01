package org.jcodec.containers.mp4;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChannelBox;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.WaveExtension;

public class AudioBoxesFactory extends BoxFactory {
    private final Map<String, Class<? extends Box>> mappings;

    public AudioBoxesFactory() {
        this.mappings = new HashMap<String, Class<? extends Box>>();
        mappings.put(WaveExtension.fourcc(), WaveExtension.class);
        mappings.put(ChannelBox.fourcc(), ChannelBox.class);
        mappings.put("esds", LeafBox.class);
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }
}