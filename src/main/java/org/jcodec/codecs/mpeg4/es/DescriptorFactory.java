package org.jcodec.codecs.mpeg4.es;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DescriptorFactory {
    private static Map<Integer, Class<? extends Descriptor>> map = new HashMap<Integer, Class<? extends Descriptor>>();
    static {
        map.put(ES.tag(), ES.class);
        map.put(SL.tag(), SL.class);
        map.put(DecoderConfig.tag(), DecoderConfig.class);
        map.put(DecoderSpecific.tag(), DecoderSpecific.class);
    }

    static DescriptorFactory factory = new DescriptorFactory();

    public Class<? extends Descriptor> byTag(int tag) {
        return map.get(tag);
    }

    public static <T> T find(Descriptor es, Class<T> class1, int tag) {
        if (es.getTag() == tag)
            return (T) es;
        else {
            if (es instanceof NodeDescriptor) {
                for (Descriptor descriptor : ((NodeDescriptor) es).getChildren()) {
                    T res = find(descriptor, class1, tag);
                    if (res != null)
                        return res;
                }
            }
        }
        return null;
    }

    public static Descriptor read(ByteBuffer input) {
        if(input.remaining() < 2)
            return null;
        int tag = input.get() & 0xff;
        int size = JCodecUtil.readBER32(input);
        
        Class<? extends Descriptor> cls = factory.byTag(tag);
        Descriptor descriptor;
        try {
            Method method = cls.getDeclaredMethod("parse", ByteBuffer.class);
            descriptor = (Descriptor)method.invoke(null, NIOUtils.read(input, size));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return descriptor;
    }
}
