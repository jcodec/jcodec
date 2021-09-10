package org.jcodec.codecs.vpx.vp8.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.jcodec.codecs.vpx.vp8.VP8Exception;
import org.jcodec.codecs.vpx.vp8.enums.AlgoFlags;
import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.enums.PacketKind;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.common.model.RationalLarge;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class CodecAlgPRiv {
    public CodecPriv base = new CodecPriv();
    public CodecEncCfg cfg;
    ExtraCFG vp8_cfg;
    public RationalLarge timestamp_ratio;
    public int pts_offset;
    public boolean pts_offset_initialized;
    public Config oxcf;
    public Compressor cpi;
    public FullAccessIntArrPointer cx_data;
    public EnumSet<FrameTypeFlags> next_frame_flag = EnumSet.noneOf(FrameTypeFlags.class);
    PostprocCfg preview_ppcfg;
    /* pkt_list size depends on the maximum number of lagged frames allowed. */
    public List<CodecPkt> pkt_list = new ArrayList<CodecPkt>();
    public int fixed_kf_cntr;
    public EnumSet<AlgoFlags> control_frame_flags = EnumSet.noneOf(AlgoFlags.class);

    public CodecAlgPRiv(CodecEncCfg cfg, ExtraCFG vp8cfg) {
        this.cfg = cfg;
        this.vp8_cfg = vp8cfg;
        vp8_cfg.setPkt_list(pkt_list);
        cx_data = new FullAccessIntArrPointer(Math.max(32768, cfg.getG_w() * cfg.getG_h() * 3 / 2 * 2));
        pts_offset_initialized = false;
        timestamp_ratio = RationalLarge.reduceLong(cfg.getG_timebase().getNum() * OnyxInt.TICKS_PER_SEC,
                cfg.getG_timebase().getDen());
        oxcf = new Config(cfg, vp8_cfg);
        cpi = new Compressor(oxcf);
        base.priv = this;
    }

    static CodecPkt vpx_codec_pkt_list_get(List<CodecPkt> list, Iterator<CodecPkt>[] iter) {
        CodecPkt pkt = null;
    
        if (iter[0] == null) {
            iter[0] = list.iterator();
        }
    
        if (iter[0].hasNext()) {
            pkt = iter[0].next();
        }
        return pkt;
    }

    public static CodecPkt vpx_codec_get_cx_data(CodecPriv ctx, Iterator<CodecPkt>[] iter) {
        CodecPkt pkt = null;
    
        if (ctx != null) {
            if (iter == null)
                VP8Exception.ERROR(null);
            else
                pkt = vpx_codec_pkt_list_get(ctx.priv.pkt_list, iter);
        }
    
        if (pkt != null && pkt.kind == PacketKind.FRAME_PKT) {
            // If the application has specified a destination area for the
            // compressed data, and the codec has not placed the data there,
            // and it fits, copy it.
            CodecPkt.FramePacket pktReal = (CodecPkt.FramePacket) pkt.packet;
            if (ctx.enc.cx_data_dst_buf != null) {
                FullAccessIntArrPointer dst_buf = ctx.enc.cx_data_dst_buf.shallowCopy();
    
                if (!pktReal.buf.equals(dst_buf) && pktReal.sz + ctx.enc.cx_data_pad_before
                        + ctx.enc.cx_data_pad_after <= ctx.enc.cx_data_dst_buf.size()) {
                    CodecPkt modified_pkt = ctx.enc.cx_data_pkt;
                    dst_buf.memcopyin(ctx.enc.cx_data_pad_before, ((CodecPkt.FramePacket) modified_pkt.packet).buf,
                            0, ((CodecPkt.FramePacket) modified_pkt.packet).buf.size());
                    modified_pkt = pkt;
                    ((CodecPkt.FramePacket) (modified_pkt.packet)).buf = dst_buf;
                    pkt = modified_pkt;
                }
    
                if (dst_buf == pktReal.buf) {
                    ctx.enc.cx_data_dst_buf = dst_buf.shallowCopyWithPosInc(pktReal.buf.size());
                }
            }
        }
    
        return pkt;
    }
}
