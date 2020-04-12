package org.jcodec.codecs.png

object PNGConsts {
    const val PNGSIG = -0x76afb1b8f2f5e5f6L
    const val PNGSIGhi = -0x76afb1b9
    const val MNGSIGhi = -0x75b2b1b9
    const val PNGSIGlo = 0x0d0a1a0a
    const val MNGSIGlo = 0x0d0a1a0a
    const val TAG_IHDR = 0x49484452
    const val TAG_IDAT = 0x49444154
    const val TAG_PLTE = 0x504c5445
    const val TAG_tRNS = 0x74524e53
    const val TAG_IEND = 0x49454e44
}