package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.CAVLCReader;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Concats AVC tracks, special logic to merge codec private is applied
 * 
 * @author The JCodec project
 * 
 */
public class AVCConcatTrack implements VirtualTrack {

    private VirtualTrack[] tracks;
    private int idx = 0;
    private VirtualPacket lastPacket;
    private double offsetPts = 0;
    private int offsetFn = 0;
    private SampleEntry se;
    private AvcCBox[] avcCs;
    private SeqParameterSet[] spss;
    private PictureParameterSet[] ppss;
    private static SliceHeaderReader shr = new SliceHeaderReader();
    private static SliceHeaderWriter shw = new SliceHeaderWriter();

    public AVCConcatTrack(VirtualTrack[] tracks) {
        this.tracks = tracks;

        List<ByteBuffer> outSps = new ArrayList<ByteBuffer>();
        List<ByteBuffer> outPps = new ArrayList<ByteBuffer>();
        spss = new SeqParameterSet[tracks.length];
        ppss = new PictureParameterSet[tracks.length];
        avcCs = new AvcCBox[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            SampleEntry se = tracks[i].getSampleEntry();
            if (!(se instanceof VideoSampleEntry))
                throw new RuntimeException("Not a video track.");
            if (!"avc1".equals(se.getFourcc()))
                throw new RuntimeException("Not an AVC track.");
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) se);
            if (avcC.getSpsList().size() > 1)
                throw new RuntimeException("Multiple SPS per track not supported.");
            if (avcC.getPpsList().size() > 1)
                throw new RuntimeException("Multiple PPS per track not supported.");
            outSps.add(changeSPS(i, avcC.getSpsList().get(0)));
            outPps.add(changePPS(i, avcC.getPpsList().get(0)));
            avcCs[i] = avcC;
        }
        se = H264Utils.createMOVSampleEntry(outSps, outPps);
    }

    private ByteBuffer changeSPS(int newId, ByteBuffer bb) {
        ByteBuffer obb = ByteBuffer.allocate(bb.remaining() + 8);
        SeqParameterSet sps = SeqParameterSet.read(bb.duplicate());
        sps.seq_parameter_set_id = newId;
        System.out.println(newId);
        sps.write(obb);
        obb.flip();
        spss[newId] = sps;
        return obb;
    }

    private ByteBuffer changePPS(int newId, ByteBuffer bb) {
        ByteBuffer obb = ByteBuffer.allocate(bb.remaining() + 8);
        PictureParameterSet pps = PictureParameterSet.read(bb.duplicate());
        pps.seq_parameter_set_id = newId;
        pps.pic_parameter_set_id = newId;
        System.out.println(newId);
        pps.write(obb);
        obb.flip();
        ppss[newId] = pps;
        return obb;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        while (idx < tracks.length) {
            VirtualTrack track = tracks[idx];
            VirtualPacket nextPacket = track.nextPacket();
            if (nextPacket == null) {
                idx++;
                offsetPts += lastPacket.getPts() + lastPacket.getDuration();
                offsetFn += lastPacket.getFrameNo() + 1;
            } else {
                lastPacket = nextPacket;
                return new AVCConcatPacket(nextPacket, offsetPts, offsetFn, idx);
            }
        }
        return null;
    }

    @Override
    public SampleEntry getSampleEntry() {
        return se;
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return tracks[0].getPreferredTimescale();
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < tracks.length; i++) {
            tracks[i].close();
        }
    }

    private ByteBuffer patchPacket(int idx2, ByteBuffer data) {
        ByteBuffer out = ByteBuffer.allocate(data.remaining() + 8);

        for (ByteBuffer nal : H264Utils.splitMOVPacket(data, avcCs[idx2])) {
            NALUnit nu = NALUnit.read(nal);

            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {

                ByteBuffer savePoint = out.duplicate();
                out.putInt(0);
                nu.write(out);
                if (!ppss[idx2].entropy_coding_mode_flag)
                    copyCAVLC(nal, out, idx2);
                else
                    copyCABAC(nal, out, idx2, nu, spss[idx2], ppss[idx2]);

                savePoint.putInt(out.position() - savePoint.position() - 4);
            }
        }
        if (out.remaining() >= 5) {
            out.putInt(out.remaining() - 4);
            new NALUnit(NALUnitType.FILLER_DATA, 0).write(out);
        }

        out.clear();

        return out;
    }

    private static void copyCAVLC(ByteBuffer is, ByteBuffer os, int id) {
        H264Utils.unescapeNAL(is);

        BitReader reader = new BitReader(is);
        BitWriter writer = new BitWriter(os);

        // SH: first_mb_in_slice
        CAVLCWriter.writeUE(writer, CAVLCReader.readUE(reader));
        // SH: slice_type
        CAVLCWriter.writeUE(writer, CAVLCReader.readUE(reader));
        // SH: pic_parameter_set_id
        CAVLCReader.readUE(reader);
        // SH: pic_parameter_set_id
        CAVLCWriter.writeUE(writer, id);

        // Byte align the writer
        int wLeft = 8 - writer.curBit();
        if (wLeft != 0)
            writer.writeNBit(reader.readNBit(wLeft), wLeft);
        writer.flush();

        // Copy with shift
        int shift = reader.curBit();
        if (shift != 0) {
            short prev = (short) (os.position() >= 2 ? os.get(os.position() - 2) & 0xff : 0xff);
            prev <<= 8;
            prev |= (short) (os.position() >= 1 ? os.get(os.position() - 1) & 0xff : 0xff);

            int mShift = 8 - shift;
            int inp = reader.readNBit(mShift);
            reader.stop();

            while (is.hasRemaining()) {
                int out = inp << shift;
                inp = is.get() & 0xff;
                out |= inp >> mShift;

                prev = outByte(os, prev, out);
            }
            outByte(os, prev, inp << shift);
        } else {
            H264Utils.escapeNAL(is, os);
        }
    }

    private static final short outByte(ByteBuffer os, short prev, int out) {
        out &= 0xff;

        if (prev == 0 && (out & ~3) == 0) {
            os.put((byte) 3);
            prev = 3;
        }
        os.put((byte) out);
        prev <<= 8;
        prev |= out;

        return prev;
    }

    private static void copyCABAC(ByteBuffer is, ByteBuffer os, int id, NALUnit nu, SeqParameterSet sps,
            PictureParameterSet pps) {
        BitReader reader = new BitReader(is);
        BitWriter writer = new BitWriter(os);
        SliceHeader sh = shr.readPart1(reader);
        shr.readPart2(sh, nu, sps, pps, reader);

        sh.pic_parameter_set_id = id;
        shw.write(sh, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, writer);

        long bp = reader.curBit();
        if (bp != 0) {
            long rem = reader.readNBit(8 - (int) bp);
            if((1 << (8 - bp)) - 1 != rem)
                throw new RuntimeException("Invalid CABAC padding");
        }

        if (writer.curBit() != 0)
            writer.writeNBit(0xff, 8 - writer.curBit());
        writer.flush();
        reader.stop();
        os.put(is);
    }

    public class AVCConcatPacket implements VirtualPacket {
        private VirtualPacket packet;
        private double ptsOffset;
        private int fnOffset;
        private int idx;

        public AVCConcatPacket(VirtualPacket packet, double ptsOffset, int fnOffset, int idx) {
            this.packet = packet;
            this.ptsOffset = ptsOffset;
            this.fnOffset = fnOffset;
            this.idx = idx;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            return patchPacket(idx, packet.getData());
        }

        @Override
        public int getDataLen() throws IOException {
            return packet.getDataLen() + 8;
        }

        @Override
        public double getPts() {
            return ptsOffset + packet.getPts();
        }

        @Override
        public double getDuration() {
            return packet.getDuration();
        }

        @Override
        public boolean isKeyframe() {
            return packet.isKeyframe();
        }

        @Override
        public int getFrameNo() {
            return fnOffset + packet.getFrameNo();
        }
    }
}