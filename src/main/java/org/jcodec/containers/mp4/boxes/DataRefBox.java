package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.io.ReaderBE;

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

    public DataRefBox() {
        this(new Header(fourcc()));
    }

    private DataRefBox(Header atom) {
        super(atom);
        factory = FACTORY;
    }

    @Override
    public void parse(InputStream input) throws IOException {
        ReaderBE.readInt32(input);
        ReaderBE.readInt32(input);
        super.parse(input);
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        out.writeInt(0);
        out.writeInt(boxes.size());
        super.doWrite(out);
    }

    public static class MyFactory extends BoxFactory {
        private Map<String, Class<? extends Box>> mappings = new HashMap<String, Class<? extends Box>>();

        public MyFactory() {
            mappings.put(UrlBox.fourcc(), UrlBox.class);
            mappings.put(AliasBox.fourcc(), AliasBox.class);
        }

        public Class<? extends Box> toClass(String fourcc) {
            return mappings.get(fourcc);
        }
    }
}
