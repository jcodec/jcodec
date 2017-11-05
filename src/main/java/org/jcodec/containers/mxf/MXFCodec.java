package org.jcodec.containers.mxf;

import static org.jcodec.common.Codec.DV;
import static org.jcodec.common.Codec.J2K;
import static org.jcodec.common.Codec.MPEG2;
import static org.jcodec.common.Codec.MPEG4;
import static org.jcodec.containers.mxf.model.UL.newUL;

import org.jcodec.common.Codec;
import org.jcodec.containers.mxf.model.UL;

public enum MXFCodec {
    //    private final static List<MXFCodec> _values = new ArrayList<MXFCodec>();

    MPEG2_XDCAM("06.0E.2B.34.04.01.01.03.04.01.02.02.01.04.03", MPEG2),

    MPEG2_ML("06.0E.2B.34.04.01.01.03.04.01.02.02.01.01.11", MPEG2),

    MPEG2_D10_PAL("06.0E.2B.34.04.01.01.01.04.01.02.02.01.02.01.01", MPEG2),

    MPEG2_HL("06.0E.2B.34.04.01.01.03.04.01.02.02.01.03.03", MPEG2),

    MPEG2_HL_422_I("06.0E.2B.34.04.01.01.03.04.01.02.02.01.04.02", MPEG2),

    MPEG4_XDCAM_PROXY("06.0E.2B.34.04.01.01.03.04.01.02.02.01.20.02.03", MPEG4),

    DV_25_PAL("06.0E.2B.34.04.01.01.01.04.01.02.02.02.01.02", DV),

    JPEG2000("06.0E.2B.34.04.01.01.07.04.01.02.02.03.01.01", J2K),

    VC1("06.0e.2b.34.04.01.01.0A.04.01.02.02.04", Codec.VC1),

    RAW("06.0E.2B.34.04.01.01.01.04.01.02.01.7F", null),

    /* uncompressed 422 8-bit */
    RAW_422("06.0E.2B.34.04.01.01.0A.04.01.02.01.01.02.01", null),

    VC3_DNXHD("06.0E.2B.34.04.01.01.01.04.01.02.02.03.02",
            Codec.VC3), VC3_DNXHD_2("06.0E.2B.34.04.01.01.01.04.01.02.02.71", Codec.VC3),
    /* SMPTE VC-3/DNxHD Legacy Avid Media Composer MXF */
    VC3_DNXHD_AVID("06.0E.2B.34.04.01.01.01.0E.04.02.01.02.04.01", Codec.VC3),

    AVC_INTRA("06.0E.2B.34.04.01.01.0A.04.01.02.02.01.32", Codec.H264),

    /* H.264/MPEG-4 AVC SPS/PPS in-band */
    AVC_SPSPPS("06.0E.2B.34.04.01.01.0A.04.01.02.02.01.31.11.01", Codec.H264),

    V210("06.0E.2B.34.04.01.01.0A.04.01.02.01.01.02.02", Codec.V210),

    /* Avid MC7 ProRes */
    PRORES_AVID("06.0E.2B.34.04.01.01.01.0E.04.02.01.02.11", Codec.PRORES),
    /* Apple ProRes */
    PRORES("06.0E.2B.34.04.01.01.0D.04.01.02.02.03.06", Codec.PRORES),

    PCM_S16LE_1("06.0E.2B.34.04.01.01.01.04.02.02.01", null),

    PCM_S16LE_3("06.0E.2B.34.04.01.01.01.04.02.02.01.01", null),

    PCM_S16LE_2("06.0E.2B.34.04.01.01.01.04.02.02.01.7F", null),

    PCM_S16BE("06.0E.2B.34.04.01.01.07.04.02.02.01.7E", null),

    PCM_ALAW("06.0E.2B.34.04.01.01.04.04.02.02.02.03.01.01", Codec.ALAW),

    AC3("06.0E.2B.34.04.01.01.01.04.02.02.02.03.02.01", Codec.AC3),

    MP2("06.0E.2B.34.04.01.01.01.04.02.02.02.03.02.05", Codec.MP3),

    UNKNOWN(new UL(new byte[0]), null);

    private final UL ul;
    private final Codec codec;

    MXFCodec(String ul, Codec codec) {
        this(newUL(ul), codec);
    }

    MXFCodec(UL ul, Codec codec) {
        this.ul = ul;
        this.codec = codec;
    }

    public UL getUl() {
        return ul;
    }

    public Codec getCodec() {
        return codec;
    }
}