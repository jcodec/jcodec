package org.jcodec.codecs.vpx;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Iterator;

import org.jcodec.codecs.vpx.vp8.CXInterface;
import org.jcodec.codecs.vpx.vp8.data.CodecAlgPRiv;
import org.jcodec.codecs.vpx.vp8.data.CodecEncCfg;
import org.jcodec.codecs.vpx.vp8.data.CodecPkt;
import org.jcodec.codecs.vpx.vp8.data.ExtraCFG;
import org.jcodec.codecs.vpx.vp8.enums.AlgoFlags;
import org.jcodec.codecs.vpx.vp8.enums.GeneralFrameFlags;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VP8Encoder extends VideoEncoder {
    public static final ColorSpace[] supportedColorSpaces = new ColorSpace[] { ColorSpace.YUV420 };

    public final static short INT_TO_BYTE_OFFSET = 128;

    CodecEncCfg cfg;
    ExtraCFG vp8Cfg;
    CodecAlgPRiv ctx;
    int pts = 0;
    int deadline = CXInterface.VPX_DL_REALTIME;

    public static VP8Encoder createVP8Encoder(short qp) {
        return new VP8Encoder(qp);
    }

    public VP8Encoder() {
        this((short) -1);
    }

    public VP8Encoder(short qp) {
        cfg = new CodecEncCfg();
        vp8Cfg = new ExtraCFG();
        if (qp >= 0) {
            setQp(qp);
        }
    }

    public void setQp(int qp) {
        cfg.setRc_max_quantizer((short) qp);
    }

    public void setScmode(int scmode) {
        vp8Cfg.setScreen_content_mode(scmode);
    }

    public void setHinter(String hinter) {
        vp8Cfg.setHinterId(hinter);
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    @Override
    public EncodedFrame encodeFrame(Picture pic, ByteBuffer _buf) {
        if (ctx == null) {
            cfg.setG_w(pic.getWidth());
            cfg.setG_h(pic.getHeight());
            ctx = new CodecAlgPRiv(cfg, vp8Cfg);
        }

        CXInterface.vp8e_encode(ctx, pic, pts++, 1, EnumSet.noneOf(AlgoFlags.class), deadline);
        CodecPkt ret = ctx.vpx_codec_get_cx_data(ctx.base, new Iterator[1]);
        CodecPkt.FramePacket fp = (CodecPkt.FramePacket) ret.packet;

        ByteBuffer out = _buf.duplicate();
        for (int i = 0; i < fp.sz; i++) {
            out.put((byte) fp.buf.getRel(i));
        }
        out.flip();
        return new EncodedFrame(out, fp.flags.contains(GeneralFrameFlags.FRAME_IS_KEY));
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return supportedColorSpaces;
    }

    @Override
    public int estimateBufferSize(Picture frame) {
        return (frame.getWidth() * frame.getHeight()) >> 1;
    }

    @Override
    public void finish() {
        // TODO Auto-generated method stub
    }
}
