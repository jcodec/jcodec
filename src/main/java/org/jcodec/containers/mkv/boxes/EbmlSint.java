package org.jcodec.containers.mkv.boxes;
import org.jcodec.containers.mkv.util.EbmlUtil;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlSint extends EbmlBin {

    public EbmlSint(byte[] id) {
        super(id);
    }

    public void setLong(long value) {
        this._data = ByteBuffer.wrap(convertToBytes(value));
    }

    public long getLong() {
        if ((_data.limit()- _data.position()) == 8)
            return _data.duplicate().getLong();
        
        byte[] b = _data.array();
        long l = 0;
        for (int i = b.length-1; i >=0 ; i--)
          l |= (b[i] & 0xFFL) << (8*(b.length-1-i));

        return l;
    }
    
    public static int ebmlSignedLength(long val) {
        if (val <= 0x40 && val >= (-0x3F)) {
            return 1;
        } else if (val <= 0x2000 && val >= (-0x1FFF)) {
            return 2;
        } else if (val <= 0x100000 && val >= (-0x0FFFFF)) {
            return 3;
        } else if (val <= 0x8000000 && val >= (-0x07FFFFFF)) {
            return 4;
        } else if (val <= 0x400000000L && val >= -0x03FFFFFFFFL) {
            return 5;
        } else if (val <= 0x20000000000L && val >= -0x01FFFFFFFFFFL) {
            return 6;
        } else if (val <= 0x1000000000000l && val >= -0x00FFFFFFFFFFFFL) {
            return 7;
        } else {
            return 8;
        }
    }

    public static final long[] signedComplement = { 0, 0x3F, 0x1FFF, 0x0FFFFF, 0x07FFFFFF, 0x03FFFFFFFFL, 0x01FFFFFFFFFFL, 0x00FFFFFFFFFFFFL, 0x007FFFFFFFFFFFFFL };

    public static byte[] convertToBytes(long val) {
        int num = ebmlSignedLength(val);
        val += signedComplement[num];

        return EbmlUtil.ebmlEncodeLen(val, num);
    }
}
