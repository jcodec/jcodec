package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class DataRefBox extends NodeBox {

    private static final MyFactory FACTORY = new MyFactory();

    public static String fourcc() {
        return "dref";
    }

    public static DataRefBox createDataRefBox() {
        return new DataRefBox(new Header(fourcc()));
    }

    public DataRefBox(Header atom) {
        super(atom);
        factory = FACTORY;
    }

    @Override
    public void parse(ByteBuffer input) {
        input.getInt();
        input.getInt();
        super.parse(input);
    }

    @Override
    public void doWrite(ByteBuffer out) {
        out.putInt(0);
        out.putInt(boxes.size());
        super.doWrite(out);
    }

    public static class MyFactory extends BoxFactory {
        private final Map<String, Class<? extends Box>> mappings;

        public MyFactory() {
            this.mappings = new HashMap<String, Class<? extends Box>>();
            mappings.put(UrlBox.fourcc(), UrlBox.class);
            mappings.put(AliasBox.fourcc(), AliasBox.class);
            mappings.put("cios", AliasBox.class);
        }

        public Class<? extends Box> toClass(String fourcc) {
            return mappings.get(fourcc);
        }
    }
}
