package org.jcodec.containers.mxf;

import static org.jcodec.common.Codec.DV;
import static org.jcodec.common.Codec.J2K;
import static org.jcodec.common.Codec.MPEG2;
import static org.jcodec.common.Codec.MPEG4;
import static org.jcodec.containers.mxf.model.UL.newUL;

import org.jcodec.common.Codec;
import org.jcodec.containers.mxf.model.UL;

public class MXFCodec {

    public static MXFCodec[] values() {
        return new MXFCodec[]{MPEG2_XDCAM, MPEG2_ML, MPEG2_D10_PAL, MPEG2_HL, MPEG2_HL_422_I, MPEG4_XDCAM_PROXY,
                DV_25_PAL, JPEG2000, VC1, RAW, RAW_422, VC3_DNXHD, VC3_DNXHD_2, VC3_DNXHD_AVID, AVC_INTRA, AVC_SPSPPS,
                V210, PRORES_AVID, PRORES, PCM_S16LE_1, PCM_S16LE_3, PCM_S16LE_2, PCM_S16BE, PCM_ALAW, AC3, MP2
        };
    }

    public final static MXFCodec MPEG2_XDCAM = new MXFCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.04.03", MPEG2);
    public final static MXFCodec MPEG2_ML = new MXFCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.01.11", MPEG2);
    public final static MXFCodec MPEG2_D10_PAL = new MXFCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.01.02.01.01", MPEG2);
    public final static MXFCodec MPEG2_HL = new MXFCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.03.03", MPEG2);
    public final static MXFCodec MPEG2_HL_422_I = new MXFCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.04.02", MPEG2);
    public final static MXFCodec MPEG4_XDCAM_PROXY = new MXFCodec("06.0E.2B.34.04.01.01.03.04.01.02.02.01.20.02.03", MPEG4);
    public final static MXFCodec DV_25_PAL = new MXFCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.02.01.02", DV);
    public final static MXFCodec JPEG2000 = new MXFCodec("06.0E.2B.34.04.01.01.07.04.01.02.02.03.01.01", J2K);
    public final static MXFCodec VC1 = new MXFCodec("06.0e.2b.34.04.01.01.0A.04.01.02.02.04", Codec.VC1);
    public final static MXFCodec RAW = new MXFCodec("06.0E.2B.34.04.01.01.01.04.01.02.01.7F", null);
    /* uncompressed 422 8-bit */
    public final static MXFCodec RAW_422 = new MXFCodec("06.0E.2B.34.04.01.01.0A.04.01.02.01.01.02.01", null);

    public final static MXFCodec VC3_DNXHD = new MXFCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.03.02", Codec.VC3);
    public final static MXFCodec VC3_DNXHD_2 = new MXFCodec("06.0E.2B.34.04.01.01.01.04.01.02.02.71", Codec.VC3);
    /* SMPTE VC-3/DNxHD Legacy Avid Media Composer MXF */
    public final static MXFCodec VC3_DNXHD_AVID = new MXFCodec("06.0E.2B.34.04.01.01.01.0E.04.02.01.02.04.01", Codec.VC3);
    public final static MXFCodec AVC_INTRA = new MXFCodec("06.0E.2B.34.04.01.01.0A.04.01.02.02.01.32", Codec.H264);

    /* H.264/MPEG-4 AVC SPS/PPS in-band */
    public final static MXFCodec AVC_SPSPPS = new MXFCodec("06.0E.2B.34.04.01.01.0A.04.01.02.02.01.31.11.01", Codec.H264);
    public final static MXFCodec V210 = new MXFCodec("06.0E.2B.34.04.01.01.0A.04.01.02.01.01.02.02", Codec.V210);

    /* Avid MC7 ProRes */
    public final static MXFCodec PRORES_AVID = new MXFCodec("06.0E.2B.34.04.01.01.01.0E.04.02.01.02.11", Codec.PRORES);
    /* Apple ProRes */
    public final static MXFCodec PRORES = new MXFCodec("06.0E.2B.34.04.01.01.0D.04.01.02.02.03.06", Codec.PRORES);
    public final static MXFCodec PCM_S16LE_1 = new MXFCodec("06.0E.2B.34.04.01.01.01.04.02.02.01", null);
    public final static MXFCodec PCM_S16LE_3 = new MXFCodec("06.0E.2B.34.04.01.01.01.04.02.02.01.01", null);
    public final static MXFCodec PCM_S16LE_2 = new MXFCodec("06.0E.2B.34.04.01.01.01.04.02.02.01.7F", null);
    public final static MXFCodec PCM_S16BE = new MXFCodec("06.0E.2B.34.04.01.01.07.04.02.02.01.7E", null);
    public final static MXFCodec PCM_ALAW = new MXFCodec("06.0E.2B.34.04.01.01.04.04.02.02.02.03.01.01", Codec.ALAW);
    public final static MXFCodec AC3 = new MXFCodec("06.0E.2B.34.04.01.01.01.04.02.02.02.03.02.01", Codec.AC3);
    public final static MXFCodec MP2 = new MXFCodec("06.0E.2B.34.04.01.01.01.04.02.02.02.03.02.05", Codec.MP3);
    public final static MXFCodec UNKNOWN = new MXFCodec(new UL(new byte[0]), null);

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