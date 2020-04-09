package org.jcodec.containers.mxf

import org.jcodec.common.Codec
import org.jcodec.containers.mxf.model.UL

class MXFCodec internal constructor(val ul: UL, val codec: Codec?) {

    companion object {
        fun mxfCodec(ul: String, codec: Codec?): MXFCodec {
            return MXFCodec(UL.newUL(ul), codec)
        }

        val MPEG2_XDCAM = mxfCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.04.03", Codec.MPEG2)
        val MPEG2_ML = mxfCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.01.11", Codec.MPEG2)
        val MPEG2_D10_PAL = mxfCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.01.02.01.01", Codec.MPEG2)
        val MPEG2_HL = mxfCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.03.03", Codec.MPEG2)
        val MPEG2_HL_422_I = mxfCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.04.02", Codec.MPEG2)
        val MPEG4_XDCAM_PROXY = mxfCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.20.02.03", Codec.MPEG4)
        val DV_25_PAL = mxfCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.02.01.02", Codec.DV)
        val JPEG2000 = mxfCodec("06.0E.2B.34.04.01.01.07.04.01.02.02.03.01.01", Codec.J2K)
        val VC1 = mxfCodec("06.0e.2b.34.04.01.01.0A.04.01.02.02.04", Codec.VC1)
        val RAW = mxfCodec("06.0E.2B.34.04.01.01.01.04.01.02.01.7F", null)

        /* uncompressed 422 8-bit */
        val RAW_422 = mxfCodec("06.0E.2B.34.04.01.01.0A.04.01.02.01.01.02.01", null)
        val VC3_DNXHD = mxfCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.03.02", Codec.VC3)
        val VC3_DNXHD_2 = mxfCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.71", Codec.VC3)

        /* SMPTE VC-3/DNxHD Legacy Avid Media Composer MXF */
        val VC3_DNXHD_AVID = mxfCodec("06.0E.2B.34.04.01.01.01.0E.04.02.01.02.04.01", Codec.VC3)
        val AVC_INTRA = mxfCodec("06.0E.2B.34.04.01.01.0A.04.01.02.02.01.32", Codec.H264)

        /* H.264/MPEG-4 AVC SPS/PPS in-band */
        val AVC_SPSPPS = mxfCodec("06.0E.2B.34.04.01.01.0A.04.01.02.02.01.31.11.01", Codec.H264)
        val V210 = mxfCodec("06.0E.2B.34.04.01.01.0A.04.01.02.01.01.02.02", Codec.V210)

        /* Avid MC7 ProRes */
        val PRORES_AVID = mxfCodec("06.0E.2B.34.04.01.01.01.0E.04.02.01.02.11", Codec.PRORES)

        /* Apple ProRes */
        val PRORES = mxfCodec("06.0E.2B.34.04.01.01.0D.04.01.02.02.03.06", Codec.PRORES)
        val PCM_S16LE_1 = mxfCodec("06.0E.2B.34.04.01.01.01.04.02.02.01", null)
        val PCM_S16LE_3 = mxfCodec("06.0E.2B.34.04.01.01.01.04.02.02.01.01", null)
        val PCM_S16LE_2 = mxfCodec("06.0E.2B.34.04.01.01.01.04.02.02.01.7F", null)
        val PCM_S16BE = mxfCodec("06.0E.2B.34.04.01.01.07.04.02.02.01.7E", null)
        val PCM_ALAW = mxfCodec("06.0E.2B.34.04.01.01.04.04.02.02.02.03.01.01", Codec.ALAW)
        val AC3 = mxfCodec("06.0E.2B.34.04.01.01.01.04.02.02.02.03.02.01", Codec.AC3)
        val MP2 = mxfCodec("06.0E.2B.34.04.01.01.01.04.02.02.02.03.02.05", Codec.MP3)
        val UNKNOWN = MXFCodec(UL(ByteArray(0)), null)
        @JvmStatic
        fun values(): Array<MXFCodec> {
            return arrayOf(MPEG2_XDCAM, MPEG2_ML, MPEG2_D10_PAL, MPEG2_HL, MPEG2_HL_422_I, MPEG4_XDCAM_PROXY,
                    DV_25_PAL, JPEG2000, VC1, RAW, RAW_422, VC3_DNXHD, VC3_DNXHD_2, VC3_DNXHD_AVID, AVC_INTRA, AVC_SPSPPS,
                    V210, PRORES_AVID, PRORES, PCM_S16LE_1, PCM_S16LE_3, PCM_S16LE_2, PCM_S16BE, PCM_ALAW, AC3, MP2
            )
        }
    }

}