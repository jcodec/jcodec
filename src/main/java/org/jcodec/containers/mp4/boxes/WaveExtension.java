package org.jcodec.containers.mp4.boxes;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.mpeg4.mp4.EsdsBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Wave extension to audio sample entry
 * 
 * @author The JCodec project
 * 
 */
public class WaveExtension extends NodeBox {
    private static final MyFactory FACTORY = new MyFactory();

    public static String fourcc() {
        return "wave";
    }

    public WaveExtension(Header atom) {
        super(atom);
        factory = FACTORY;

    }

    public static class MyFactory extends BoxFactory {
        private Map<String, Class<? extends Box>> mappings = new HashMap<String, Class<? extends Box>>();

        public MyFactory() {
            mappings.put(FormatBox.fourcc(), FormatBox.class);
            mappings.put(EndianBox.fourcc(), EndianBox.class);
//            mappings.put(EsdsBox.fourcc(), EsdsBox.class);
        }

        public Class<? extends Box> toClass(String fourcc) {
            return mappings.get(fourcc);
        }
    }
}