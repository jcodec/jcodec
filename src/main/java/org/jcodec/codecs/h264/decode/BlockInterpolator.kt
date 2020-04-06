package org.jcodec.codecs.h264.decode

import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Interpolator that operates on block level
 *
 * @author The JCodec project
 */
class BlockInterpolator {
    private val tmp1: IntArray
    private val tmp2: IntArray
    private val tmp3: ByteArray
    private val safe: Array<LumaInterpolator>
    private val unsafe: Array<LumaInterpolator>

    /**
     * Get block of ( possibly interpolated ) luma pixels
     */
    fun getBlockLuma(pic: Picture, out: Picture, off: Int, x: Int, y: Int, w: Int, h: Int) {
        val xInd = x and 0x3
        val yInd = y and 0x3
        val xFp = x shr 2
        val yFp = y shr 2
        if (xFp < 2 || yFp < 2 || xFp > pic.width - w - 5 || yFp > pic.height - h - 5) {
            unsafe[(yInd shl 2) + xInd].getLuma(pic.data[0], pic.width, pic.height, out.getPlaneData(0),
                    off, out.getPlaneWidth(0), xFp, yFp, w, h)
        } else {
            safe[(yInd shl 2) + xInd].getLuma(pic.data[0], pic.width, pic.height, out.getPlaneData(0),
                    off, out.getPlaneWidth(0), xFp, yFp, w, h)
        }
    }

    fun getLuma20Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        getLuma20UnsafeNoRound(pic, picW, picH, tmp1, blkOff, blkStride, x, y, blkW, blkH)
        for (i in 0 until blkW) {
            var boff = blkOff
            for (j in 0 until blkH) {
                blk[boff + i] = MathUtil.clip(tmp1[boff + i] + 16 shr 5, -128, 127).toByte()
                boff += blkStride
            }
        }
    }

    fun getLuma02Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma02UnsafeNoRound(pic, picW, picH, tmp1, blkOff, blkStride, x, y, blkW, blkH)
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                blk[blkOff + i] = MathUtil.clip(tmp1[blkOff + i] + 16 shr 5, -128, 127).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Qpel: (1,0) horizontal unsafe
     */
    fun getLuma10Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val maxH = picH - 1
        val maxW = picW - 1
        getLuma20Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH)
        for (j in 0 until blkH) {
            val lineStart = MathUtil.clip(j + y, 0, maxH) * picW
            for (i in 0 until blkW) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + MathUtil.clip(x + i, 0, maxW)] + 1 shr 1).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Qpel horizontal (3, 0) unsafe
     */
    fun getLuma30Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val maxH = picH - 1
        val maxW = picW - 1
        getLuma20Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH)
        for (j in 0 until blkH) {
            val lineStart = MathUtil.clip(j + y, 0, maxH) * picW
            for (i in 0 until blkW) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + MathUtil.clip(x + i + 1, 0, maxW)] + 1 shr 1).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Qpel vertical (0, 1) unsafe
     */
    fun getLuma01Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val maxH = picH - 1
        val maxW = picW - 1
        getLuma02Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH)
        for (j in 0 until blkH) {
            val lineStart = MathUtil.clip(y + j, 0, maxH) * picW
            for (i in 0 until blkW) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + MathUtil.clip(x + i, 0, maxW)] + 1 shr 1).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Qpel vertical (0, 3) unsafe
     */
    fun getLuma03Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val maxH = picH - 1
        val maxW = picW - 1
        getLuma02Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH)
        for (j in 0 until blkH) {
            val lineStart = MathUtil.clip(y + j + 1, 0, maxH) * picW
            for (i in 0 until blkW) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + MathUtil.clip(x + i, 0, maxW)] + 1 shr 1).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Hpel horizontal, Qpel vertical (2, 1)
     *
     */
    fun getLuma21(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7)
        getLuma02NoRoundInt(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH)
        var off = blkW shl 1
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += blkW
        }
    }

    /**
     * Qpel vertical (2, 1) unsafe
     */
    fun getLuma21Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7)
        getLuma02NoRoundInt(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH)
        var off = blkW shl 1
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += blkW
        }
    }

    /**
     * Hpel horizontal, Hpel vertical (2, 2)
     */
    fun getLuma22(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7)
        getLuma02NoRoundInt(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH)
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                blk[blkOff + i] = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Hpel (2, 2) unsafe
     */
    fun getLuma22Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7)
        getLuma02NoRoundInt(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH)
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                blk[blkOff + i] = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127).toByte()
            }
            blkOff += blkStride
        }
    }

    /**
     * Hpel horizontal, Qpel vertical (2, 3)
     *
     */
    fun getLuma23(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7)
        getLuma02NoRoundInt(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH)
        var off = blkW shl 1
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i + blkW] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += blkW
        }
    }

    /**
     * Qpel (2, 3) unsafe
     */
    fun getLuma23Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7)
        getLuma02NoRoundInt(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH)
        var off = blkW shl 1
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i + blkW] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += blkW
        }
    }

    /**
     * Qpel horizontal, Hpel vertical (1, 2)
     */
    fun getLuma12(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val tmpW = blkW + 7
        getLuma02NoRound(pic, picW, tmp1, 0, tmpW, x - 2, y, tmpW, blkH)
        getLuma20NoRoundInt(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH)
        var off = 2
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += tmpW
        }
    }

    /**
     * Qpel (1, 2) unsafe
     */
    fun getLuma12Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val tmpW = blkW + 7
        getLuma02UnsafeNoRound(pic, picW, imgH, tmp1, 0, tmpW, x - 2, y, tmpW, blkH)
        getLuma20NoRoundInt(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH)
        var off = 2
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += tmpW
        }
    }

    /**
     * Qpel horizontal, Hpel vertical (3, 2)
     */
    fun getLuma32(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val tmpW = blkW + 7
        getLuma02NoRound(pic, picW, tmp1, 0, tmpW, x - 2, y, tmpW, blkH)
        getLuma20NoRoundInt(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH)
        var off = 2
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i + 1] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += tmpW
        }
    }

    /**
     * Qpel (3, 2) unsafe
     */
    fun getLuma32Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        var blkOff = blkOff
        val tmpW = blkW + 7
        getLuma02UnsafeNoRound(pic, picW, imgH, tmp1, 0, tmpW, x - 2, y, tmpW, blkH)
        getLuma20NoRoundInt(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH)
        var off = 2
        for (j in 0 until blkH) {
            for (i in 0 until blkW) {
                val rounded = MathUtil.clip(tmp2[blkOff + i] + 512 shr 10, -128, 127)
                val rounded2 = MathUtil.clip(tmp1[off + i + 1] + 16 shr 5, -128, 127)
                blk[blkOff + i] = (rounded + rounded2 + 1 shr 1).toByte()
            }
            blkOff += blkStride
            off += tmpW
        }
    }

    /**
     * Qpel horizontal, Qpel vertical (3, 3)
     */
    fun getLuma33(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y + 1, blkW, blkH)
        getLuma02(pic, picW, tmp3, 0, blkW, x + 1, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel (3, 3) unsafe
     */
    fun getLuma33Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y + 1, blkW, blkH)
        getLuma02Unsafe(pic, picW, imgH, tmp3, 0, blkW, x + 1, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel horizontal, Qpel vertical (1, 1)
     */
    fun getLuma11(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
        getLuma02(pic, picW, tmp3, 0, blkW, x, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel (1, 1) unsafe
     */
    fun getLuma11Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
        getLuma02Unsafe(pic, picW, imgH, tmp3, 0, blkW, x, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel horizontal, Qpel vertical (1, 3)
     */
    fun getLuma13(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y + 1, blkW, blkH)
        getLuma02(pic, picW, tmp3, 0, blkW, x, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel (1, 3) unsafe
     */
    fun getLuma13Unsafe(pic: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y + 1, blkW, blkH)
        getLuma02Unsafe(pic, picW, imgH, tmp3, 0, blkW, x, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel horizontal, Qpel vertical (3, 1)
     */
    fun getLuma31(pels: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int,
                  blkH: Int) {
        getLuma20(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
        getLuma02(pels, picW, tmp3, 0, blkW, x + 1, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    /**
     * Qpel (3, 1) unsafe
     */
    fun getLuma31Unsafe(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                        blkW: Int, blkH: Int) {
        getLuma20Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
        getLuma02Unsafe(pels, picW, imgH, tmp3, 0, blkW, x + 1, y, blkW, blkH)
        merge(blk, tmp3, blkOff, blkStride, blkW, blkH)
    }

    private interface LumaInterpolator {
        fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int,
                    blkH: Int)
    }

    private fun initSafe(): Array<LumaInterpolator> {
        val self = this
        return arrayOf(
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        getLuma00(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        getLuma10(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        getLuma20(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                getLuma30(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                getLuma01(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma11(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma21(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma31(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                getLuma02(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma12(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma22(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma32(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                getLuma03(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma13(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma23(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma33(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        })
    }

    private fun initUnsafe(): Array<LumaInterpolator> {
        val self = this
        return arrayOf(
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        getLuma00Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma10Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma20Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma30Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma01Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma11Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma21Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma31Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma02Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma12Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma22Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma32Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma03Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma13Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                },
                object : LumaInterpolator {
                    override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                         blkW: Int, blkH: Int) {
                        self.getLuma23Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
                    }
                }, object : LumaInterpolator {
            override fun getLuma(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                 blkW: Int, blkH: Int) {
                self.getLuma33Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH)
            }
        })
    }

    companion object {
        @JvmStatic
        fun getBlockChroma(pels: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                           blkW: Int, blkH: Int) {
            val xInd = x and 0x7
            val yInd = y and 0x7
            val xFull = x shr 3
            val yFull = y shr 3
            if (xFull < 0 || xFull > picW - blkW - 1 || yFull < 0 || yFull > picH - blkH - 1) {
                if (xInd == 0 && yInd == 0) {
                    getChroma00Unsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, blkW, blkH)
                } else if (yInd == 0) {
                    getChromaX0Unsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, blkW, blkH)
                } else if (xInd == 0) {
                    getChroma0XUnsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, yInd, blkW, blkH)
                } else {
                    getChromaXXUnsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, yInd, blkW, blkH)
                }
            } else {
                if (xInd == 0 && yInd == 0) {
                    getChroma00(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, blkW, blkH)
                } else if (yInd == 0) {
                    getChromaX0(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, blkW, blkH)
                } else if (xInd == 0) {
                    getChroma0X(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, yInd, blkW, blkH)
                } else {
                    getChromaXX(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, yInd, blkW, blkH)
                }
            }
        }

        /**
         * Fullpel (0, 0)
         */
        fun getLuma00(pic: ByteArray?, picW: Int, blk: ByteArray?, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var off = y * picW + x
            for (j in 0 until blkH) {
                System.arraycopy(pic, off, blk, blkOff, blkW)
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Fullpel (0, 0) unsafe
         */
        fun getLuma00Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                            blkW: Int, blkH: Int) {
            var blkOff = blkOff
            val maxH = picH - 1
            val maxW = picW - 1
            for (j in 0 until blkH) {
                val lineStart = MathUtil.clip(j + y, 0, maxH) * picW
                for (i in 0 until blkW) {
                    blk[blkOff + i] = pic[lineStart + MathUtil.clip(x + i, 0, maxW)]
                }
                blkOff += blkStride
            }
        }

        /**
         * Halfpel (2,0) horizontal, int argument version
         */
        fun getLuma20NoRoundInt(pic: IntArray, picW: Int, blk: IntArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int,
                                blkH: Int) {
            var blkOff = blkOff
            var off = y * picW + x
            for (j in 0 until blkH) {
                var off1 = -2
                for (i in 0 until blkW) {
                    val a = pic[off + off1] + pic[off + off1 + 5]
                    val b = pic[off + off1 + 1] + pic[off + off1 + 4]
                    val c = pic[off + off1 + 2] + pic[off + off1 + 3]
                    blk[blkOff + i] = a + 5 * ((c shl 2) - b)
                    ++off1
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Halfpel (2,0) horizontal
         */
        fun getLuma20NoRound(pic: ByteArray, picW: Int, blk: IntArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int,
                             blkH: Int) {
            var blkOff = blkOff
            var off = y * picW + x
            for (j in 0 until blkH) {
                var off1 = -2
                for (i in 0 until blkW) {
                    val a = pic[off + off1] + pic[off + off1 + 5]
                    val b = pic[off + off1 + 1] + pic[off + off1 + 4]
                    val c = pic[off + off1 + 2] + pic[off + off1 + 3]
                    blk[blkOff + i] = a + 5 * ((c shl 2) - b)
                    ++off1
                }
                off += picW
                blkOff += blkStride
            }
        }

        fun getLuma20(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var off = y * picW + x
            for (j in 0 until blkH) {
                var off1 = -2
                for (i in 0 until blkW) {
                    val a = pic[off + off1] + pic[off + off1 + 5]
                    val b = pic[off + off1 + 1] + pic[off + off1 + 4]
                    val c = pic[off + off1 + 2] + pic[off + off1 + 3]
                    blk[blkOff + i] = MathUtil.clip(a + 5 * ((c shl 2) - b) + 16 shr 5, -128, 127).toByte()
                    ++off1
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Halfpel (2, 0) horizontal unsafe
         */
        fun getLuma20UnsafeNoRound(pic: ByteArray, picW: Int, picH: Int, blk: IntArray, blkOff: Int, blkStride: Int, x: Int,
                                   y: Int, blkW: Int, blkH: Int) {
            val maxW = picW - 1
            val maxH = picH - 1
            for (i in 0 until blkW) {
                val ipos_m2 = MathUtil.clip(x + i - 2, 0, maxW)
                val ipos_m1 = MathUtil.clip(x + i - 1, 0, maxW)
                val ipos = MathUtil.clip(x + i, 0, maxW)
                val ipos_p1 = MathUtil.clip(x + i + 1, 0, maxW)
                val ipos_p2 = MathUtil.clip(x + i + 2, 0, maxW)
                val ipos_p3 = MathUtil.clip(x + i + 3, 0, maxW)
                var boff = blkOff
                for (j in 0 until blkH) {
                    val lineStart = MathUtil.clip(j + y, 0, maxH) * picW
                    val a = pic[lineStart + ipos_m2] + pic[lineStart + ipos_p3]
                    val b = pic[lineStart + ipos_m1] + pic[lineStart + ipos_p2]
                    val c = pic[lineStart + ipos] + pic[lineStart + ipos_p1]
                    blk[boff + i] = a + 5 * ((c shl 2) - b)
                    boff += blkStride
                }
            }
        }

        /**
         * Halfpel (0, 2) vertical
         */
        fun getLuma02NoRoundInt(pic: IntArray, picW: Int, blk: IntArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int,
                                blkH: Int) {
            var blkOff = blkOff
            var off = (y - 2) * picW + x
            val picWx2 = picW + picW
            val picWx3 = picWx2 + picW
            val picWx4 = picWx3 + picW
            val picWx5 = (picWx4
                    + picW)
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    val a = pic[off + i] + pic[off + i + picWx5]
                    val b = pic[off + i + picW] + pic[off + i + picWx4]
                    val c = pic[off + i + picWx2] + pic[off + i + picWx3]
                    blk[blkOff + i] = a + 5 * ((c shl 2) - b)
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Halfpel (0, 2) vertical
         */
        fun getLuma02NoRound(pic: ByteArray, picW: Int, blk: IntArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int,
                             blkH: Int) {
            var blkOff = blkOff
            var off = (y - 2) * picW + x
            val picWx2 = picW + picW
            val picWx3 = picWx2 + picW
            val picWx4 = picWx3 + picW
            val picWx5 = (picWx4
                    + picW)
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    val a = pic[off + i] + pic[off + i + picWx5]
                    val b = pic[off + i + picW] + pic[off + i + picWx4]
                    val c = pic[off + i + picWx2] + pic[off + i + picWx3]
                    blk[blkOff + i] = a + 5 * ((c shl 2) - b)
                }
                off += picW
                blkOff += blkStride
            }
        }

        fun getLuma02(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var off = (y - 2) * picW + x
            val picWx2 = picW + picW
            val picWx3 = picWx2 + picW
            val picWx4 = picWx3 + picW
            val picWx5 = (picWx4
                    + picW)
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    val a = pic[off + i] + pic[off + i + picWx5]
                    val b = pic[off + i + picW] + pic[off + i + picWx4]
                    val c = pic[off + i + picWx2] + pic[off + i + picWx3]
                    blk[blkOff + i] = MathUtil.clip(a + 5 * ((c shl 2) - b) + 16 shr 5, -128, 127).toByte()
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Hpel (0, 2) vertical unsafe
         */
        fun getLuma02UnsafeNoRound(pic: ByteArray, picW: Int, picH: Int, blk: IntArray, blkOff: Int, blkStride: Int, x: Int,
                                   y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            val maxH = picH - 1
            val maxW = picW - 1
            for (j in 0 until blkH) {
                val offP0 = MathUtil.clip(y + j - 2, 0, maxH) * picW
                val offP1 = MathUtil.clip(y + j - 1, 0, maxH) * picW
                val offP2 = MathUtil.clip(y + j, 0, maxH) * picW
                val offP3 = MathUtil.clip(y + j + 1, 0, maxH) * picW
                val offP4 = MathUtil.clip(y + j + 2, 0, maxH) * picW
                val offP5 = MathUtil.clip(y + j + 3, 0, maxH) * picW
                for (i in 0 until blkW) {
                    val pres_x = MathUtil.clip(x + i, 0, maxW)
                    val a = pic[pres_x + offP0] + pic[pres_x + offP5]
                    val b = pic[pres_x + offP1] + pic[pres_x + offP4]
                    val c = pic[pres_x + offP2] + pic[pres_x + offP3]
                    blk[blkOff + i] = a + 5 * ((c shl 2) - b)
                }
                blkOff += blkStride
            }
        }

        /**
         * Qpel: (1,0) horizontal
         */
        fun getLuma10(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            var off = y * picW + x
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (blk[blkOff + i] + pic[off + i] + 1 shr 1).toByte()
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Qpel (3,0) horizontal
         */
        fun getLuma30(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            var off = y * picW + x
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (pic[off + i + 1] + blk[blkOff + i] + 1 shr 1).toByte()
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Qpel vertical (0, 1)
         */
        fun getLuma01(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            getLuma02(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            var off = y * picW + x
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (blk[blkOff + i] + pic[off + i] + 1 shr 1).toByte()
                }
                off += picW
                blkOff += blkStride
            }
        }

        /**
         * Qpel vertical (0, 3)
         */
        fun getLuma03(pic: ByteArray, picW: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            getLuma02(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH)
            var off = y * picW + x
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (blk[blkOff + i] + pic[off + i + picW] + 1 shr 1).toByte()
                }
                off += picW
                blkOff += blkStride
            }
        }

        private fun merge(first: ByteArray, second: ByteArray, blkOff: Int, blkStride: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var tOff = 0
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    first[blkOff + i] = (first[blkOff + i] + second[tOff + i] + 1 shr 1).toByte()
                }
                blkOff += blkStride
                tOff += blkW
            }
        }

        /**
         * Chroma (0,0)
         */
        private fun getChroma00(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var off = y * picW + x
            for (j in 0 until blkH) {
                System.arraycopy(pic, off, blk, blkOff, blkW)
                off += picW
                blkOff += blkStride
            }
        }

        private fun getChroma00Unsafe(pic: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, x: Int, y: Int,
                                      blkW: Int, blkH: Int) {
            var blkOff = blkOff
            val maxH = picH - 1
            val maxW = picW - 1
            for (j in 0 until blkH) {
                val lineStart = MathUtil.clip(j + y, 0, maxH) * picW
                for (i in 0 until blkW) {
                    blk[blkOff + i] = pic[lineStart + MathUtil.clip(x + i, 0, maxW)]
                }
                blkOff += blkStride
            }
        }

        /**
         * Chroma (X,0)
         */
        private fun getChroma0X(pels: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, fullX: Int,
                                fullY: Int, fracY: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var w00 = fullY * picW + fullX
            var w01 = w00 + if (fullY < picH - 1) picW else 0
            val eMy = 8 - fracY
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (eMy * pels[w00 + i] + fracY * pels[w01 + i] + 4 shr 3).toByte()
                }
                w00 += picW
                w01 += picW
                blkOff += blkStride
            }
        }

        private fun getChroma0XUnsafe(pels: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, fullX: Int,
                                      fullY: Int, fracY: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            val maxW = picW - 1
            val maxH = picH - 1
            val eMy = 8 - fracY
            for (j in 0 until blkH) {
                val off00 = MathUtil.clip(fullY + j, 0, maxH) * picW
                val off01 = MathUtil.clip(fullY + j + 1, 0, maxH) * picW
                for (i in 0 until blkW) {
                    val w00 = MathUtil.clip(fullX + i, 0, maxW) + off00
                    val w01 = MathUtil.clip(fullX + i, 0, maxW) + off01
                    blk[blkOff + i] = (eMy * pels[w00] + fracY * pels[w01] + 4 shr 3).toByte()
                }
                blkOff += blkStride
            }
        }

        /**
         * Chroma (X,0)
         */
        private fun getChromaX0(pels: ByteArray, picW: Int, imgH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, fullX: Int,
                                fullY: Int, fracX: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var w00 = fullY * picW + fullX
            var w10 = w00 + if (fullX < picW - 1) 1 else 0
            val eMx = 8 - fracX
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (eMx * pels[w00 + i] + fracX * pels[w10 + i] + 4 shr 3).toByte()
                }
                w00 += picW
                w10 += picW
                blkOff += blkStride
            }
        }

        private fun getChromaX0Unsafe(pels: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, fullX: Int,
                                      fullY: Int, fracX: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            val eMx = 8 - fracX
            val maxW = picW - 1
            val maxH = picH - 1
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    val w00 = MathUtil.clip(fullY + j, 0, maxH) * picW + MathUtil.clip(fullX + i, 0, maxW)
                    val w10 = MathUtil.clip(fullY + j, 0, maxH) * picW + MathUtil.clip(fullX + i + 1, 0, maxW)
                    blk[blkOff + i] = (eMx * pels[w00] + fracX * pels[w10] + 4 shr 3).toByte()
                }
                blkOff += blkStride
            }
        }

        /**
         * Chroma (X,X)
         */
        private fun getChromaXX(pels: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, fullX: Int,
                                fullY: Int, fracX: Int, fracY: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            var w00 = fullY * picW + fullX
            var w01 = w00 + if (fullY < picH - 1) picW else 0
            var w10 = w00 + if (fullX < picW - 1) 1 else 0
            var w11 = w10 + w01 - w00
            val eMx = 8 - fracX
            val eMy = 8 - fracY
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    blk[blkOff + i] = (eMx * eMy * pels[w00 + i] + fracX * eMy * pels[w10 + i] + (eMx * fracY
                            * pels[w01 + i]) + fracX * fracY * pels[w11 + i] + 32 shr 6).toByte()
                }
                blkOff += blkStride
                w00 += picW
                w01 += picW
                w10 += picW
                w11 += picW
            }
        }

        private fun getChromaXXUnsafe(pels: ByteArray, picW: Int, picH: Int, blk: ByteArray, blkOff: Int, blkStride: Int, fullX: Int,
                                      fullY: Int, fracX: Int, fracY: Int, blkW: Int, blkH: Int) {
            var blkOff = blkOff
            val maxH = picH - 1
            val maxW = picW - 1
            val eMx = 8 - fracX
            val eMy = 8 - fracY
            for (j in 0 until blkH) {
                for (i in 0 until blkW) {
                    val w00 = MathUtil.clip(fullY + j, 0, maxH) * picW + MathUtil.clip(fullX + i, 0, maxW)
                    val w01 = MathUtil.clip(fullY + j + 1, 0, maxH) * picW + MathUtil.clip(fullX + i, 0, maxW)
                    val w10 = MathUtil.clip(fullY + j, 0, maxH) * picW + MathUtil.clip(fullX + i + 1, 0, maxW)
                    val w11 = MathUtil.clip(fullY + j + 1, 0, maxH) * picW + MathUtil.clip(fullX + i + 1, 0, maxW)
                    blk[blkOff + i] = (eMx * eMy * pels[w00] + fracX * eMy * pels[w10] + eMx * fracY * pels[w01] + fracX * fracY * pels[w11] + 32 shr 6).toByte()
                }
                blkOff += blkStride
            }
        }
    }

    init {
        tmp1 = IntArray(1024)
        tmp2 = IntArray(1024)
        tmp3 = ByteArray(1024)
        safe = initSafe()
        unsafe = initUnsafe()
    }
}