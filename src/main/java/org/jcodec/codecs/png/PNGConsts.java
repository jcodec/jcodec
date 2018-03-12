package org.jcodec.codecs.png;

public class PNGConsts {
    static final long PNGSIG = 0x89504e470d0a1a0aL;
    static final int PNGSIGhi = 0x89504e47;
    static final int MNGSIGhi = 0x8a4d4e47;
    static final int PNGSIGlo = 0x0d0a1a0a;
    static final int MNGSIGlo = 0x0d0a1a0a;
    static final int TAG_IHDR = 0x49484452;
    static final int TAG_IDAT = 0x49444154;
    static final int TAG_PLTE = 0x504c5445;
    static final int TAG_tRNS = 0x74524e53;
    static final int TAG_IEND = 0x49454e44;
}
