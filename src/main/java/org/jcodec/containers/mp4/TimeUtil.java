package org.jcodec.containers.mp4;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtil {

    public final static long MOV_TIME_OFFSET;
    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.set(1904, 0, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        MOV_TIME_OFFSET = calendar.getTimeInMillis();
    }

    public static Date macTimeToDate(int movSec) {
        return new Date(fromMovTime(movSec));
    }

    public static long fromMovTime(int movSec) {
        return ((long)movSec) * 1000L + MOV_TIME_OFFSET;
    }

    public static int toMovTime(long millis) {
        return (int)((millis - MOV_TIME_OFFSET) / 1000L);
    }
}