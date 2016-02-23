package org.jcodec.movtool.streaming.tracks.avc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.H264Utils.SliceHeaderTweaker;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VideoCodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;
import org.jcodec.movtool.streaming.tracks.ClipTrack;
import org.jcodec.movtool.streaming.tracks.VirtualPacketWrapper;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Clips AVC track replacing the remainder of a GOP at cut point with I-frames
 * 
 * @author The JCodec project
 * 
 */
public class AVCClipTrack extends ClipTrack {

//    private AvcCBox avcC;
    private H264FixedRateControl rc;
    private int mbW;
    private int mbH;
    private VideoCodecMeta se;
    private final int frameSize;
    private SeqParameterSet encSPS;
    private PictureParameterSet encPPS;
    private byte[] codecPrivate;

    public AVCClipTrack(VirtualTrack src, int frameFrom, int frameTo) {
        super(src, frameFrom, frameTo);

        VideoCodecMeta codecMeta = (VideoCodecMeta)src.getCodecMeta();
        if (!"avc1".equals(codecMeta.getFourcc()))
            throw new RuntimeException("Not an AVC source track");

        rc = new H264FixedRateControl(1024);
        H264Encoder encoder = new H264Encoder(rc);
        ByteBuffer codecPrivate = codecMeta.getCodecPrivate();
        this.codecPrivate = NIOUtils.toArray(codecPrivate);
        List<ByteBuffer> rawSPS = H264Utils.getRawSPS(codecPrivate);
        List<ByteBuffer> rawPPS = H264Utils.getRawPPS(codecPrivate);
        SeqParameterSet sps = H264Utils.readSPS(rawSPS.get(0));

        mbW = sps.pic_width_in_mbs_minus1 + 1;
        mbH = H264Utils.getPicHeightInMbs(sps);

        encSPS = encoder.initSPS(H264Utils.getPicSize(sps));
        encSPS.seq_parameter_set_id = 1;
        encPPS = encoder.initPPS();
        encPPS.seq_parameter_set_id = 1;
        encPPS.pic_parameter_set_id = 1;
        encSPS.profile_idc = sps.profile_idc;
        encSPS.level_idc = sps.level_idc;
        encSPS.frame_mbs_only_flag = sps.frame_mbs_only_flag;
        encSPS.frame_crop_bottom_offset = sps.frame_crop_bottom_offset;
        encSPS.frame_crop_left_offset = sps.frame_crop_left_offset;
        encSPS.frame_crop_right_offset = sps.frame_crop_right_offset;
        encSPS.frame_crop_top_offset = sps.frame_crop_top_offset;
        encSPS.vuiParams = sps.vuiParams;

        rawSPS.add(H264Utils.writeSPS(encSPS, 128));
        rawPPS.add(H264Utils.writePPS(encPPS, 20));
        
        se = new VideoCodecMeta("avc1", ByteBuffer.wrap(H264Utils.saveCodecPrivate(rawSPS, rawPPS)),
                codecMeta.getSize(), codecMeta.getPasp());

        int _frameSize = rc.calcFrameSize(mbW * mbH);
        _frameSize += _frameSize >> 4;
        this.frameSize = _frameSize;
    }

    protected List<VirtualPacket> getGop(VirtualTrack src, int from) throws IOException {
        VirtualPacket packet = src.nextPacket();

        List<VirtualPacket> head = new ArrayList<VirtualPacket>();
        while (packet != null && packet.getFrameNo() < from) {
            if (packet.isKeyframe())
                head.clear();
            head.add(packet);
            packet = src.nextPacket();
        }
        List<VirtualPacket> tail = new ArrayList<VirtualPacket>();
        while (packet != null && !packet.isKeyframe()) {
            tail.add(packet);
            packet = src.nextPacket();
        }
        
        List<VirtualPacket> gop = new ArrayList<VirtualPacket>();
        GopTranscoder tr = new GopTranscoder(this, head, tail);
        
        for (int i = 0; i < tail.size(); i++)
            gop.add(new TranscodePacket(tail.get(i), tr, i, frameSize));

        gop.add(packet);

        return gop;
    }

    public static class GopTranscoder {

        private List<VirtualPacket> tail;
        private List<VirtualPacket> head;
        private List<ByteBuffer> result;
		private AVCClipTrack track;

        public GopTranscoder(AVCClipTrack track, List<VirtualPacket> head, List<VirtualPacket> tail) {
            this.track = track;
			this.head = head;
            this.tail = tail;
        }

        public List<ByteBuffer> transcode() throws IOException {
            H264Decoder decoder = new H264Decoder(track.codecPrivate);
            Picture buf = Picture.create(track.mbW << 4, track.mbH << 4, ColorSpace.YUV420J);
            Picture dec = null;
            for (VirtualPacket virtualPacket : head) {
                dec = decoder.decodeFrame(H264Utils.splitFrame(virtualPacket.getData()), buf.getData());
            }
            H264Encoder encoder = new H264Encoder(track.rc);
            ByteBuffer tmp = ByteBuffer.allocate(track.frameSize);

            List<ByteBuffer> result = new ArrayList<ByteBuffer>();
            for (VirtualPacket pkt : tail) {
                dec = decoder.decodeFrame(H264Utils.splitFrame(pkt.getData()), buf.getData());

                tmp.clear();
                ByteBuffer res = encoder.encodeFrame(dec, tmp);
                ByteBuffer out = ByteBuffer.allocate(track.frameSize);
                processFrame(res, out);

                result.add(out);
            }

            return result;
        }

        private void processFrame(ByteBuffer _in, ByteBuffer out) {
            SliceHeaderTweaker st = new H264Utils.SliceHeaderTweaker() {
                @Override
                protected void tweak(SliceHeader sh) {
                    sh.pic_parameter_set_id = 1;
                }
            };

            ByteBuffer dup = _in.duplicate();
            while (dup.hasRemaining()) {
                ByteBuffer buf = H264Utils.nextNALUnit(dup);
                if (buf == null)
                    break;

                NALUnit nu = NALUnit.read(buf);
                if (nu.type == NALUnitType.IDR_SLICE) {
                    ByteBuffer sp = out.duplicate();
                    out.putInt(0);
                    nu.write(out);
                    st.run(buf, out, nu, track.encSPS, track.encPPS);
                    sp.putInt(out.position() - sp.position() - 4);
                }
            }

            if (out.remaining() >= 5) {
                out.putInt(out.remaining() - 4);
                new NALUnit(NALUnitType.FILLER_DATA, 0).write(out);
            }
            out.clear();
        }

        public synchronized List<ByteBuffer> getResult() throws IOException {
            if (result == null)
                result = transcode();

            return result;
        }
    }

    @Override
    public CodecMeta getCodecMeta() {
        return se;
    }

    public static class TranscodePacket extends VirtualPacketWrapper {

        private GopTranscoder tr;
        private int off;
		private int frameSize;

        public TranscodePacket(VirtualPacket src, GopTranscoder tr, int off, int frameSize) {
            super(src);

            this.tr = tr;
            this.off = off;
			this.frameSize = frameSize;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            return NIOUtils.duplicate(tr.getResult().get(off));
        }

        @Override
        public int getDataLen() throws IOException {
            return frameSize;
        }

        @Override
        public boolean isKeyframe() {
            return true;
        }
    }
}