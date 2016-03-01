package org.jcodec.containers.mp4;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.CleanApertureExtension;
import org.jcodec.containers.mp4.boxes.ColorExtension;
import org.jcodec.containers.mp4.boxes.FielExtension;
import org.jcodec.containers.mp4.boxes.GamaExtension;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;

public class VideoBoxesFactory extends BoxFactory {
    private Map<String, Class<? extends Box>> mappings;

    public VideoBoxesFactory() {
        this.mappings = new HashMap<String, Class<? extends Box>>();

        mappings.put(PixelAspectExt.fourcc(), PixelAspectExt.class);
        mappings.put(AvcCBox.fourcc(), AvcCBox.class);
        mappings.put(ColorExtension.fourcc(), ColorExtension.class);
        mappings.put(GamaExtension.fourcc(), GamaExtension.class);
        mappings.put(CleanApertureExtension.fourcc(), CleanApertureExtension.class);
        mappings.put(FielExtension.fourcc(), FielExtension.class);
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }
}