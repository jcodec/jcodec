package org.jcodec.containers.mxf.read;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MXF demuxer
 * 
 * @author The JCodec project
 * 
 */
public class MXFUtil {
    public static <T> T resolveRef(List<MXFMetadata> metadata, UL refs, Class<T> class1) {
        List<T> res = resolveRefs(metadata, new UL[] { refs }, class1);
        return res.size() > 0 ? res.get(0) : null;
    }

    public static <T> List<T> resolveRefs(List<MXFMetadata> metadata, UL[] refs, Class<T> class1) {
        List<MXFMetadata> copy = new ArrayList<MXFMetadata>(metadata);
        for (Iterator<MXFMetadata> iterator = copy.iterator(); iterator.hasNext();) {
            MXFMetadata next = iterator.next();
            if (next.getUid() == null || !class1.isAssignableFrom(next.getClass()))
                iterator.remove();
        }

        List result = new ArrayList();
        for (int i = 0; i < refs.length; i++) {
            for (MXFMetadata meta : copy) {
                if (meta.getUid().equals(refs[i])) {
                    result.add(meta);
                }
            }
        }
        return result;
    }

    public static <T> T findMeta(Collection<MXFMetadata> metadata, Class<T> class1) {
        for (MXFMetadata mxfMetadata : metadata) {
            if (mxfMetadata.getClass().isAssignableFrom(class1))
                return (T) mxfMetadata;
        }
        return null;
    }

    public static <T> List<T> findAllMeta(Collection<MXFMetadata> metadata, Class<T> class1) {
        List result = new ArrayList();
        for (MXFMetadata mxfMetadata : metadata) {
            if (mxfMetadata.getClass().isAssignableFrom(class1))
                result.add((T) mxfMetadata);
        }
        return result;
    }

}
