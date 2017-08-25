package org.jcodec.samples.mashup;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.common.Assert;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MovStitch2 {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Syntax: <in1.mov> <in2.mov> <out.mov>");
            return;
        }

        File in1 = new File(args[0]);
        File in2 = new File(args[1]);
        File out = new File(args[2]);

        changePPS(in1, in2, out);
    }

    public static void changePPS(File in1, File in2, File out) throws IOException {
        MP4Muxer muxer = MP4Muxer.createMP4Muxer(writableChannel(out), Brand.MOV);

        MP4Demuxer demuxer1 = MP4Demuxer.createMP4Demuxer(readableChannel(in1));
        DemuxerTrack vt1 = demuxer1.getVideoTrack();
        MP4Demuxer demuxer2 = MP4Demuxer.createMP4Demuxer(readableChannel(in2));
        DemuxerTrack vt2 = demuxer2.getVideoTrack();
        checkCompatible(vt1, vt2);

        DemuxerTrackMeta meta1 = vt1.getMeta();
        DemuxerTrackMeta meta2 = vt2.getMeta();
        MuxerTrack outTrack = muxer.addVideoTrack(meta1.getCodecMeta().video());
        for (int i = 0; i < meta1.getTotalFrames(); i++) {
            outTrack.addFrame((MP4Packet) vt1.nextFrame());
        }

        List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
        List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
        ByteBuffer codecPrivate2 = updateCodecPrivate(vt2.getMeta().getCodecMeta().getCodecPrivate(), spsList, ppsList);

        SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
        PictureParameterSet pps = PictureParameterSet.read(ppsList.get(0).duplicate());

        for (int i = 0; i < meta2.getTotalFrames(); i++) {
            Packet packet = vt2.nextFrame();
            ByteBuffer frm;
            if(codecPrivate2 != null) {
                frm = ByteBuffer.allocate(packet.getData().remaining() + 24 + codecPrivate2.remaining());
                frm.put(codecPrivate2);
                codecPrivate2 = null;
            } else {
                frm = ByteBuffer.allocate(packet.getData().remaining() + 24);
            }
            doFrame(packet.getData(), frm, sps, pps);
            outTrack.addFrame(Packet.createPacketWithData(packet, frm));
        }

        muxer.finish();
    }

    private static void checkCompatible(DemuxerTrack vt1, DemuxerTrack vt2) {
        DemuxerTrackMeta meta1 = vt1.getMeta();
        DemuxerTrackMeta meta2 = vt2.getMeta();

        Assert.assertTrue(meta1.getCodecMeta().getCodec() == Codec.H264);
        Assert.assertTrue(meta2.getCodecMeta().getCodec() == Codec.H264);
        Size size1 = meta1.getCodecMeta().video().getSize();
        Size size2 = meta2.getCodecMeta().video().getSize();
        Assert.assertEquals(size1.getWidth(), size2.getWidth());
        Assert.assertEquals(size1.getHeight(), size2.getHeight());
    }

    public static void doFrame(ByteBuffer data, ByteBuffer dst, SeqParameterSet sps, PictureParameterSet pps)
            throws IOException {
        SliceHeaderWriter shw = new SliceHeaderWriter();
        SliceHeaderReader shr = new SliceHeaderReader();
        while (data.remaining() > 0) {
            ByteBuffer nalUnit = H264Utils.nextNALUnit(data);

            NALUnit nu = NALUnit.read(nalUnit);
            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
                dst.getInt(1);
                copyNU(shr, shw, nu, nalUnit, dst, sps, pps);
            } else {
                dst.putInt(1);
                nu.write(dst);
                NIOUtils.write(dst, nalUnit);
            }
        }

        dst.flip();
    }

    public static void copyNU(SliceHeaderReader shr, SliceHeaderWriter shw, NALUnit nu, ByteBuffer is, ByteBuffer os,
            SeqParameterSet sps, PictureParameterSet pps) {
        BitReader reader = BitReader.createBitReader(is);
        BitWriter writer = new BitWriter(os);

        SliceHeader sh = SliceHeaderReader.readPart1(reader);
        SliceHeaderReader.readPart2(sh, nu, sps, pps, reader);
        sh.picParameterSetId = 1;

        nu.write(os);
        shw.write(sh, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, writer);

        copyCABAC(writer, reader);
    }

    private static ByteBuffer updateCodecPrivate(ByteBuffer codecPrivate, List<ByteBuffer> sps, List<ByteBuffer> pps) {
        H264Utils.wipePS(codecPrivate, null, sps, pps);

        for (int i = 0; i < pps.size(); i++) {
            pps.set(i, updatePps(pps.get(i)));
        }

        for (int i = 0; i < sps.size(); i++) {
            sps.set(i, updateSps(sps.get(i)));
        }

        return H264Utils.saveCodecPrivate(sps, pps);
    }

    private static void copyCABAC(BitWriter w, BitReader r) {
        long bp = r.curBit();
        long rem = r.readNBit(8 - (int) bp);
        Assert.assertEquals((1 << (8 - bp)) - 1, rem);

        if (w.curBit() != 0)
            w.writeNBit(0xff, 8 - w.curBit());
        int b;
        while ((b = r.readNBit(8)) != -1)
            w.writeNBit(b, 8);
    }

    static ByteBuffer updateSps(ByteBuffer bb) {
        SeqParameterSet sps = SeqParameterSet.read(bb);
        sps.seqParameterSetId = 1;
        ByteBuffer out = ByteBuffer.allocate(bb.capacity() + 10);
        sps.write(out);
        out.flip();
        return out;
    }

    static ByteBuffer updatePps(ByteBuffer bb) {
        PictureParameterSet pps = PictureParameterSet.read(bb);
        Assert.assertTrue(pps.entropyCodingModeFlag);
        pps.seqParameterSetId = 1;
        pps.picParameterSetId = 1;
        ByteBuffer out = ByteBuffer.allocate(bb.capacity() + 10);
        pps.write(out);
        out.flip();
        return out;
    }
}
