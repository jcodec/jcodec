package org.jcodec.containers.dpx;

import static java.lang.String.format;

public class DPXMetadata {
    public FileInfoHeader fileInfo;
    public ImageInformationHeader imageInfo;
    public ImageSourceInfoHeader imageSource;
    public FilmInformationHeader filmInfo;
    public TelevisionInfoHeader tvInfo;
    public String userId;

    public static String smpte_tc(int tcsmpte, boolean prevent_dropframe) {
        int ff = DPXReader.bcd2uint(tcsmpte & 0x3f);    // 6-bit hours
        int ss = DPXReader.bcd2uint(tcsmpte >> 8 & 0x7f);    // 7-bit minutes
        int mm = DPXReader.bcd2uint(tcsmpte >> 16 & 0x7f);    // 7-bit seconds
        int hh = DPXReader.bcd2uint(tcsmpte >> 24 & 0x3f);    // 6-bit frames
        boolean drop = (tcsmpte & 1 << 30) > 0L && !prevent_dropframe;  // 1-bit drop if not arbitrary bit
        return format("%02d:%02d:%02d%c%02d", hh, mm, ss, drop ? ';' : ':', ff);
    }

    public String getTimecodeString() {
        return smpte_tc(tvInfo.timecode, false);
    }

}
