package org.jcodec.containers.mkv.boxes;
import static org.jcodec.containers.mkv.util.EbmlUtil.toHexString;

import org.jcodec.containers.mkv.MKVType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import js.lang.System;
import js.nio.ByteBuffer;
import js.util.Calendar;
import js.util.Date;

public class EbmlDateTest {

    
    @Test
    public void testFewBytesToLong() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00});
        long l = bb.getLong();
        Assert.assertEquals(1, l);
    }
    
    @Ignore @Test
    public void testSet() throws Exception {
        EbmlDate d = MKVType.createByType(MKVType.DateUTC);
        d.setLong(0);
        Assert.assertEquals(getDate(1970, 1, 1), d.getDate());
        System.out.println(toHexString(d.data.array()));
        System.out.println(getDate(2001, 1, 1));
    }
    
    public static final Date getDate(int year, int month, int day){
        Calendar c = Calendar.getInstance();
        c.set(year, month-1, day);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

}
