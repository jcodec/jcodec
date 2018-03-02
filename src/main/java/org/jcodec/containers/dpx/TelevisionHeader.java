package org.jcodec.containers.dpx;

public class TelevisionHeader {
    public int timecode;
    public int userBits;
    public byte interlace; // Interlace (0 = noninterlaced; 1 = 2:1 interlace)
    public byte filedNumber;
    public byte videoSignalStarted;
    public byte zero;
    public int horSamplingRateHz;
    public int vertSampleRateHz;
    public int frameRate;
    public int timeOffset;
    public int gamma;
    public int blackLevel;
    public int blackGain;
    public int breakpoint;
    public int referenceWhiteLevel;
    public int integrationTime;
//        public byte[] reserved;

}
