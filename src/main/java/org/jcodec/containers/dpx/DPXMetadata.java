package org.jcodec.containers.dpx;

import static java.lang.String.format;

public class DPXMetadata {
    public static final String V2 = "V2.0";
    public static final String V1 = "V1.0";

    public FileHeader file;
    public ImageHeader image;
    public ImageSourceHeader imageSource;
    public FilmHeader film;
    public TelevisionHeader television;
    public String userId;

    private static String smpteTC(int tcsmpte, boolean prevent_dropframe) {
        int ff = bcd2uint(tcsmpte & 0x3f);    // 6-bit hours
        int ss = bcd2uint(tcsmpte >> 8 & 0x7f);    // 7-bit minutes
        int mm = bcd2uint(tcsmpte >> 16 & 0x7f);    // 7-bit seconds
        int hh = bcd2uint(tcsmpte >> 24 & 0x3f);    // 6-bit frames
        boolean drop = (tcsmpte & 1 << 30) > 0L && !prevent_dropframe;  // 1-bit drop if not arbitrary bit
        return format("%02d:%02d:%02d%c%02d", hh, mm, ss, drop ? ';' : ':', ff);
    }

    private static int bcd2uint(int bcd) {
        int low = bcd & 0xf;
        int high = bcd >> 4;
        if (low > 9 || high > 9)
            return 0;
        return low + 10 * high;
    }

    public String getTimecodeString() {
        return smpteTC(television.timecode, false);
    }

}
