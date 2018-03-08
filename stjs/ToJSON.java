package org.jcodec.common.tools;
import org.stjs.javascript.JSGlobal;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Simple JSON serializer, introduced because jcodec can not use dependencies as
 * they bring frastration on some platforms
 * 
 * @author The JCodec project
 */
public class ToJSON {
    
    /**
     * Converts an object to JSON
     * 
     * @param obj
     * @return
     */
    public static String toJSON(Object obj) {
        return JSGlobal.JSON.stringify(obj);
    }

}
