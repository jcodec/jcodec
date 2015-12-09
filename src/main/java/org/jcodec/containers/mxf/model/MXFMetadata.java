package org.jcodec.containers.mxf.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class MXFMetadata {
    protected UL ul;
    protected UL uid;

    public MXFMetadata(UL ul) {
        this.ul = ul;
    }

    public abstract void read(ByteBuffer bb);

    /**
     * Utility method to read a batch of ULS
     * 
     * @param _bb
     * @return
     */
    protected static UL[] readULBatch(ByteBuffer _bb) {
        int count = _bb.getInt();
        _bb.getInt();
        UL[] result = new UL[count];
        for (int i = 0; i < count; i++) {
            result[i] = UL.read(_bb);
        }
        return result;
    }
    
    /**
     * Utility method to read a batch of int32
     * 
     * @param _bb
     * @return
     */
    protected static int[] readInt32Batch(ByteBuffer _bb) {
        int count = _bb.getInt();
        _bb.getInt();
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = _bb.getInt();
        }
        return result;
    }

    protected static Date readDate(ByteBuffer _bb) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, _bb.getShort());
        calendar.set(Calendar.MONTH, _bb.get());
        calendar.set(Calendar.DAY_OF_MONTH, _bb.get());
        calendar.set(Calendar.HOUR, _bb.get());
        calendar.set(Calendar.MINUTE, _bb.get());
        calendar.set(Calendar.SECOND, _bb.get());
        calendar.set(Calendar.MILLISECOND, (_bb.get() & 0xff) << 2);

        return calendar.getTime();

    }
    
    protected String readUtf16String(ByteBuffer _bb) {
        try {
            return new String(NIOUtils.toArray(_bb), "utf-16");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
    
    public UL getUl() {
        return ul;
    }

    public UL getUid() {
        return uid;
    }
}
