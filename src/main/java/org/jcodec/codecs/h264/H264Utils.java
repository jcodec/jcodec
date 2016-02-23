package org.jcodec.codecs.h264;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class H264Utils {

    private static SliceHeaderReader shr = new SliceHeaderReader();
    private static SliceHeaderWriter shw = new SliceHeaderWriter();

    public static ByteBuffer nextNALUnit(ByteBuffer buf) {
        skipToNALUnit(buf);
        return gotoNALUnit(buf);
    }

    public static final void skipToNALUnit(ByteBuffer buf) {

        if (!buf.hasRemaining())
            return;

        int val = 0xffffffff;
        while (buf.hasRemaining()) {
            val <<= 8;
            val |= (buf.get() & 0xff);
            if ((val & 0xffffff) == 1) {
                buf.position(buf.position());
                break;
            }
        }
    }

    /**
     * Finds next Nth H.264 bitstream NAL unit (0x00000001) and returns the data
     * that preceeds it as a ByteBuffer slice
     * 
     * Segment byte order is always little endian
     * 
     * TODO: emulation prevention
     * 
     * @param buf
     * @return
     */
    public static final ByteBuffer gotoNALUnit(ByteBuffer buf) {

        if (!buf.hasRemaining())
            return null;

        int from = buf.position();
        ByteBuffer result = buf.slice();
        result.order(ByteOrder.BIG_ENDIAN);

        int val = 0xffffffff;
        while (buf.hasRemaining()) {
            val <<= 8;
            val |= (buf.get() & 0xff);
            if ((val & 0xffffff) == 1) {
                buf.position(buf.position() - (val == 1 ? 4 : 3));
                result.limit(buf.position() - from);
                break;
            }
        }
        return result;
    }

    public static final void unescapeNAL(ByteBuffer _buf) {
        if (_buf.remaining() < 2)
            return;
        ByteBuffer _in = _buf.duplicate();
        ByteBuffer out = _buf.duplicate();
        byte p1 = _in.get();
        out.put(p1);
        byte p2 = _in.get();
        out.put(p2);
        while (_in.hasRemaining()) {
            byte b = _in.get();
            if (p1 != 0 || p2 != 0 || b != 3)
                out.put(b);
            p1 = p2;
            p2 = b;
        }
        _buf.limit(out.position());
    }

    public static final void escapeNAL(ByteBuffer src) {
        int[] loc = searchEscapeLocations(src);

        int old = src.limit();
        src.limit(src.limit() + loc.length);

        for (int newPos = src.limit() - 1, oldPos = old - 1, locIdx = loc.length - 1; newPos >= src.position(); newPos--, oldPos--) {
            src.put(newPos, src.get(oldPos));
            if (locIdx >= 0 && loc[locIdx] == oldPos) {
                newPos--;
                src.put(newPos, (byte) 3);
                locIdx--;
            }
        }
    }

    private static int[] searchEscapeLocations(ByteBuffer src) {
        IntArrayList points = new IntArrayList();
        ByteBuffer search = src.duplicate();
        short p = search.getShort();
        while (search.hasRemaining()) {
            byte b = search.get();
            if (p == 0 && (b & ~3) == 0) {
                points.add(search.position() - 1);
                p = 3;
            }
            p <<= 8;
            p |= b & 0xff;
        }
        int[] array = points.toArray();
        return array;
    }

    public static final void escapeNAL(ByteBuffer src, ByteBuffer dst) {
        byte p1 = src.get(), p2 = src.get();
        dst.put(p1);
        dst.put(p2);
        while (src.hasRemaining()) {
            byte b = src.get();
            if (p1 == 0 && p2 == 0 && (b & 0xff) <= 3) {
                dst.put((byte) 3);
                p1 = p2;
                p2 = 3;
            }
            dst.put(b);
            p1 = p2;
            p2 = b;
        }
    }

    public static int golomb2Signed(int val) {
        int sign = ((val & 0x1) << 1) - 1;
        val = ((val >> 1) + (val & 0x1)) * sign;
        return val;
    }

    public static List<ByteBuffer> splitMOVPacket(ByteBuffer buf, AvcCBox avcC) {
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        int nls = avcC.getNalLengthSize();
        ByteBuffer dup = buf.duplicate();
        while (dup.remaining() >= nls) {
            int len = readLen(dup, nls);
            if (len == 0)
                break;
            result.add(NIOUtils.read(dup, len));
        }
        return result;
    }

    private static int readLen(ByteBuffer dup, int nls) {
        switch (nls) {
        case 1:
            return dup.get() & 0xff;
        case 2:
            return dup.getShort() & 0xffff;
        case 3:
            return ((dup.getShort() & 0xffff) << 8) | (dup.get() & 0xff);
        case 4:
            return dup.getInt();
        default:
            throw new IllegalArgumentException("NAL Unit length size can not be " + nls);
        }
    }

    /**
     * Encodes AVC frame in ISO BMF format. Takes Annex B format.
     * 
     * Scans the packet for each NAL Unit starting with 00 00 00 01 and replaces
     * this 4 byte sequence with 4 byte integer representing this NAL unit
     * length. Removes any leading SPS/PPS structures and collects them into a
     * provided storaae.
     * 
     * @param avcFrame
     *            AVC frame encoded in Annex B NAL unit format
     */
    public static void encodeMOVPacket(ByteBuffer avcFrame) {

        ByteBuffer dup = avcFrame.duplicate();
        ByteBuffer d1 = avcFrame.duplicate();

        for (int tot = d1.position();;) {
            ByteBuffer buf = H264Utils.nextNALUnit(dup);
            if (buf == null)
                break;
            d1.position(tot);
            d1.putInt(buf.remaining());
            tot += buf.remaining() + 4;
        }
    }
    
    /**
     * Decodes AVC packet in ISO BMF format into Annex B format.
     * 
     * Replaces NAL unit size integers with 00 00 00 01 start codes. If the
     * space allows the transformation is done inplace.
     * 
     * @param result
     */
    public static ByteBuffer decodeMOVPacket(ByteBuffer result, AvcCBox avcC) {
        if(avcC.getNalLengthSize() == 4) {
            decodeMOVPacketInplace(result, avcC);
            return result;
        }
        return joinNALUnits(splitMOVPacket(result, avcC));
    }

    /**
     * Decodes AVC packet in ISO BMF format into Annex B format.
     * 
     * Inplace replaces NAL unit size integers with 00 00 00 01 start codes. 
     * @param result
     */
    public static void decodeMOVPacketInplace(ByteBuffer result, AvcCBox avcC) {
        if (avcC.getNalLengthSize() != 4)
            throw new IllegalArgumentException("Can only inplace decode AVC MOV packet with nal_length_size = 4.");
        ByteBuffer dup = result.duplicate();
        while (dup.remaining() >= 4) {
            int size = dup.getInt();
            dup.position(dup.position() - 4);
            dup.putInt(1);
            dup.position(dup.position() + size);
        }
    }

    /**
     * Wipes AVC parameter sets ( SPS/PPS ) from the packet
     * 
     * @param in
     *            AVC frame encoded in Annex B NAL unit format
     * @param out
     *            Buffer where packet without PS will be put
     * @param spsList
     *            Storage for leading SPS structures ( can be null, then all
     *            leading SPSs are discarded ).
     * @param ppsList
     *            Storage for leading PPS structures ( can be null, then all
     *            leading PPSs are discarded ).
     */
    public static void wipePS(ByteBuffer _in, ByteBuffer out, List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {

        ByteBuffer dup = _in.duplicate();
        while (dup.hasRemaining()) {
            ByteBuffer buf = H264Utils.nextNALUnit(dup);
            if (buf == null)
                break;

            NALUnit nu = NALUnit.read(buf.duplicate());
            if (nu.type == NALUnitType.PPS) {
                if (ppsList != null)
                    ppsList.add(NIOUtils.duplicate(buf));
            } else if (nu.type == NALUnitType.SPS) {
                if (spsList != null)
                    spsList.add(NIOUtils.duplicate(buf));
            } else {
                out.putInt(1);
                out.put(buf);
            }
        }
        out.flip();
    }

    /**
     * Wipes AVC parameter sets ( SPS/PPS ) from the packet ( inplace operation
     * )
     * 
     * @param in
     *            AVC frame encoded in Annex B NAL unit format
     * @param spsList
     *            Storage for leading SPS structures ( can be null, then all
     *            leading SPSs are discarded ).
     * @param ppsList
     *            Storage for leading PPS structures ( can be null, then all
     *            leading PPSs are discarded ).
     */
    public static void wipePS(ByteBuffer _in, Collection<ByteBuffer> spsList, Collection<ByteBuffer> ppsList) {
        ByteBuffer dup = _in.duplicate();
        while (dup.hasRemaining()) {
            ByteBuffer buf = H264Utils.nextNALUnit(dup);
            if (buf == null)
                break;

            NALUnit nu = NALUnit.read(buf);
            if (nu.type == NALUnitType.PPS) {
                if (ppsList != null)
                    ppsList.add(NIOUtils.duplicate(buf));
                _in.position(dup.position());
            } else if (nu.type == NALUnitType.SPS) {
                if (spsList != null)
                    spsList.add(NIOUtils.duplicate(buf));
                _in.position(dup.position());
            } else if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE)
                break;
        }
    }

    public static AvcCBox createAvcC(SeqParameterSet sps, PictureParameterSet pps, int nalLengthSize) {
        ByteBuffer serialSps = ByteBuffer.allocate(512);
        sps.write(serialSps);
        serialSps.flip();
        H264Utils.escapeNAL(serialSps);

        ByteBuffer serialPps = ByteBuffer.allocate(512);
        pps.write(serialPps);
        serialPps.flip();
        H264Utils.escapeNAL(serialPps);

        AvcCBox avcC = new AvcCBox(sps.profile_idc, 0, sps.level_idc, nalLengthSize,
                Arrays.asList(new ByteBuffer[] { serialSps }), Arrays.asList(new ByteBuffer[] { serialPps }));

        return avcC;
    }

    public static AvcCBox createAvcC(List<SeqParameterSet> initSPS, List<PictureParameterSet> initPPS, int nalLengthSize) {
        List<ByteBuffer> serialSps = saveSPS(initSPS);
        List<ByteBuffer> serialPps = savePPS(initPPS);

        SeqParameterSet sps = initSPS.get(0);
        return new AvcCBox(sps.profile_idc, 0, sps.level_idc, nalLengthSize, serialSps, serialPps);
    }

    /**
     * @param initPPS
     * @return
     */
    public static List<ByteBuffer> savePPS(List<PictureParameterSet> initPPS) {
        List<ByteBuffer> serialPps = new ArrayList<ByteBuffer>();
        for (PictureParameterSet pps : initPPS) {
            ByteBuffer bb1 = ByteBuffer.allocate(512);
            pps.write(bb1);
            bb1.flip();
            H264Utils.escapeNAL(bb1);
            serialPps.add(bb1);
        }
        return serialPps;
    }

    /**
     * @param initSPS
     * @return
     */
    public static List<ByteBuffer> saveSPS(List<SeqParameterSet> initSPS) {
        List<ByteBuffer> serialSps = new ArrayList<ByteBuffer>();
        for (SeqParameterSet sps : initSPS) {
            ByteBuffer bb1 = ByteBuffer.allocate(512);
            sps.write(bb1);
            bb1.flip();
            H264Utils.escapeNAL(bb1);
            serialSps.add(bb1);
        }
        return serialSps;
    }

    /**
     * Creates a MP4 sample entry given AVC/H.264 codec private.
     * @param codecPrivate Array containing AnnexB delimited (00 00 00 01) SPS/PPS NAL units.
     * @return MP4 sample entry
     */
    public static SampleEntry createMOVSampleEntry(byte[] codecPrivate) {
        List<ByteBuffer> rawSPS = getRawSPS(ByteBuffer.wrap(codecPrivate));
        List<ByteBuffer> rawPPS = getRawPPS(ByteBuffer.wrap(codecPrivate));
        return createMOVSampleEntry(rawSPS, rawPPS, 4);
    }
    
    public static SampleEntry createMOVSampleEntry(List<ByteBuffer> spsList, List<ByteBuffer> ppsList, int nalLengthSize) {
        SeqParameterSet sps = readSPS(NIOUtils.duplicate(spsList.get(0)));
        AvcCBox avcC = new AvcCBox(sps.profile_idc, 0, sps.level_idc, nalLengthSize, spsList, ppsList);

        return createMOVSampleEntry(avcC);
    }

    public static SampleEntry createMOVSampleEntry(AvcCBox avcC) {
        SeqParameterSet sps = SeqParameterSet.read(avcC.getSpsList().get(0).duplicate());
        int codedWidth = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int codedHeight = getPicHeightInMbs(sps) << 4;

        int width = sps.frame_cropping_flag ? codedWidth
                - ((sps.frame_crop_right_offset + sps.frame_crop_left_offset) << sps.chroma_format_idc.compWidth[1])
                : codedWidth;
        int height = sps.frame_cropping_flag ? codedHeight
                - ((sps.frame_crop_bottom_offset + sps.frame_crop_top_offset) << sps.chroma_format_idc.compHeight[1])
                : codedHeight;

        Size size = new Size(width, height);

        SampleEntry se = MP4Muxer.videoSampleEntry("avc1", size, "JCodec");
        se.add(avcC);
        return se;
    }

    public static SampleEntry createMOVSampleEntry(SeqParameterSet initSPS, PictureParameterSet initPPS,
            int nalLengthSize) {
        ByteBuffer bb1 = ByteBuffer.allocate(512), bb2 = ByteBuffer.allocate(512);
        initSPS.write(bb1);
        initPPS.write(bb2);
        bb1.flip();
        bb2.flip();
        return createMOVSampleEntry(bb1, bb2, nalLengthSize);
    }

    public static SampleEntry createMOVSampleEntry(ByteBuffer sps, ByteBuffer pps, int nalLengthSize) {
        return createMOVSampleEntry(Arrays.asList(new ByteBuffer[] { sps }), Arrays.asList(new ByteBuffer[] { pps }),
                nalLengthSize);
    }

    public static boolean idrSlice(ByteBuffer _data) {
        ByteBuffer data = _data.duplicate();
        ByteBuffer segment;
        while ((segment = H264Utils.nextNALUnit(data)) != null) {
            if (NALUnit.read(segment).type == NALUnitType.IDR_SLICE)
                return true;
        }
        return false;
    }

    public static boolean idrSlice(List<ByteBuffer> _data) {
        for (ByteBuffer segment : _data) {
            if (NALUnit.read(segment.duplicate()).type == NALUnitType.IDR_SLICE)
                return true;
        }
        return false;
    }

    public static void saveRawFrame(ByteBuffer data, AvcCBox avcC, File f) throws IOException {
        SeekableByteChannel raw = NIOUtils.writableChannel(f);
        saveStreamParams(avcC, raw);
        raw.write(data.duplicate());
        raw.close();
    }

    public static void saveStreamParams(AvcCBox avcC, SeekableByteChannel raw) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        for (ByteBuffer byteBuffer : avcC.getSpsList()) {
            raw.write(ByteBuffer.wrap(new byte[] { 0, 0, 0, 1, 0x67 }));

            H264Utils.escapeNAL(byteBuffer.duplicate(), bb);
            bb.flip();
            raw.write(bb);
            bb.clear();
        }
        for (ByteBuffer byteBuffer : avcC.getPpsList()) {
            raw.write(ByteBuffer.wrap(new byte[] { 0, 0, 0, 1, 0x68 }));
            H264Utils.escapeNAL(byteBuffer.duplicate(), bb);
            bb.flip();
            raw.write(bb);
            bb.clear();
        }
    }

    public static int getPicHeightInMbs(SeqParameterSet sps) {
        int picHeightInMbs = (sps.pic_height_in_map_units_minus1 + 1) << (sps.frame_mbs_only_flag ? 0 : 1);
        return picHeightInMbs;
    }

    public static List<ByteBuffer> splitFrame(ByteBuffer frame) {
        ArrayList<ByteBuffer> result = new ArrayList<ByteBuffer>();

        ByteBuffer segment;
        while ((segment = H264Utils.nextNALUnit(frame)) != null) {
            result.add(segment);
        }

        return result;
    }

    /**
     * Joins buffers containing individual NAL units into a single AnnexB delimited buffer.
     * Each NAL unit will be separated with 00 00 00 01 markers. Allocates a new byte buffer
     * and writes data into it. 
     * @param nalUnits
     * @param out
     */
    public static ByteBuffer joinNALUnits(List<ByteBuffer> nalUnits) {
        int size = 0;
        for (ByteBuffer nal : nalUnits) {
            size += 4 + nal.remaining();
        }
        ByteBuffer allocate = ByteBuffer.allocate(size);
        joinNALUnits(nalUnits, allocate);
        return allocate;
    }
    
    /**
     * Joins buffers containing individual NAL units into a single AnnexB delimited buffer.
     * Each NAL unit will be separated with 00 00 00 01 markers. 
     * @param nalUnits
     * @param out
     */
    public static void joinNALUnits(List<ByteBuffer> nalUnits, ByteBuffer out) {
        for (ByteBuffer nal : nalUnits) {
            out.putInt(1);
            out.put(nal.duplicate());
        }
    }

    public static ByteBuffer getAvcCData(AvcCBox avcC) {
        ByteBuffer bb = ByteBuffer.allocate(2048);
        avcC.doWrite(bb);
        bb.flip();
        return bb;
    }

    public static AvcCBox parseAVCC(VideoSampleEntry vse) {
        Box lb = Box.findFirst(vse, Box.class, "avcC");
        if (lb instanceof AvcCBox)
            return (AvcCBox) lb;
        else {
            return parseAVCC(((LeafBox) lb).getData().duplicate());
        }
    }
    
    public static byte[] saveCodecPrivate(List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {
        int totalCodecPrivateSize = 0;
        for (ByteBuffer byteBuffer : spsList) {
            totalCodecPrivateSize += byteBuffer.remaining() + 5;
        }
        for (ByteBuffer byteBuffer : ppsList) {
            totalCodecPrivateSize += byteBuffer.remaining() + 5;
        }
        
        ByteBuffer bb = ByteBuffer.allocate(totalCodecPrivateSize);
        for (ByteBuffer byteBuffer : spsList) {
            bb.putInt(1);
            bb.put((byte)0x67);
            bb.put(byteBuffer.duplicate());
        }
        for (ByteBuffer byteBuffer : ppsList) {
            bb.putInt(1);
            bb.put((byte)0x68);
            bb.put(byteBuffer.duplicate());
        }
        bb.flip();
        return NIOUtils.toArray(bb);
    }
    
    public static byte[] avcCToAnnexB(AvcCBox avcC) {
        return saveCodecPrivate(avcC.getSpsList(), avcC.getPpsList());
    }

    public static AvcCBox parseAVCC(ByteBuffer bb) {
        AvcCBox avcC = new AvcCBox();
        avcC.parse(bb);
        return avcC;
    }

    public static ByteBuffer writeSPS(SeqParameterSet sps, int approxSize) {
        ByteBuffer output = ByteBuffer.allocate(approxSize + 8);
        sps.write(output);
        output.flip();
        H264Utils.escapeNAL(output);
        return output;
    }

    public static SeqParameterSet readSPS(ByteBuffer data) {
        ByteBuffer input = NIOUtils.duplicate(data);
        H264Utils.unescapeNAL(input);
        SeqParameterSet sps = SeqParameterSet.read(input);
        return sps;
    }

    public static ByteBuffer writePPS(PictureParameterSet pps, int approxSize) {
        ByteBuffer output = ByteBuffer.allocate(approxSize + 8);
        pps.write(output);
        output.flip();
        H264Utils.escapeNAL(output);
        return output;
    }

    public static PictureParameterSet readPPS(ByteBuffer data) {
        ByteBuffer input = NIOUtils.duplicate(data);
        H264Utils.unescapeNAL(input);
        PictureParameterSet pps = PictureParameterSet.read(input);
        return pps;
    }

    public static PictureParameterSet findPPS(List<PictureParameterSet> ppss, int id) {
        for (PictureParameterSet pps : ppss) {
            if (pps.pic_parameter_set_id == id)
                return pps;
        }
        return null;
    }

    public static SeqParameterSet findSPS(List<SeqParameterSet> spss, int id) {
        for (SeqParameterSet sps : spss) {
            if (sps.seq_parameter_set_id == id)
                return sps;
        }
        return null;
    }

    public abstract static class SliceHeaderTweaker {

        private List<SeqParameterSet> sps;
        private List<PictureParameterSet> pps;

        public SliceHeaderTweaker() {
        }

        public SliceHeaderTweaker(List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {
            this.sps = readSPS(spsList);
            this.pps = readPPS(ppsList);
        }

        protected abstract void tweak(SliceHeader sh);

        public SliceHeader run(ByteBuffer is, ByteBuffer os, NALUnit nu) {
            ByteBuffer nal = os.duplicate();

            H264Utils.unescapeNAL(is);

            BitReader reader = new BitReader(is);
            SliceHeader sh = shr.readPart1(reader);

            PictureParameterSet pp = findPPS(pps, sh.pic_parameter_set_id);

            return part2(is, os, nu, findSPS(sps, pp.pic_parameter_set_id), pp, nal, reader, sh);
        }

        public SliceHeader run(ByteBuffer is, ByteBuffer os, NALUnit nu, SeqParameterSet sps, PictureParameterSet pps) {
            ByteBuffer nal = os.duplicate();

            H264Utils.unescapeNAL(is);

            BitReader reader = new BitReader(is);
            SliceHeader sh = shr.readPart1(reader);

            return part2(is, os, nu, sps, pps, nal, reader, sh);
        }

        private SliceHeader part2(ByteBuffer is, ByteBuffer os, NALUnit nu, SeqParameterSet sps,
                PictureParameterSet pps, ByteBuffer nal, BitReader reader, SliceHeader sh) {
            BitWriter writer = new BitWriter(os);
            shr.readPart2(sh, nu, sps, pps, reader);

            tweak(sh);

            shw.write(sh, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, writer);

            if (pps.entropy_coding_mode_flag)
                copyDataCABAC(is, os, reader, writer);
            else
                copyDataCAVLC(is, os, reader, writer);

            nal.limit(os.position());

            H264Utils.escapeNAL(nal);

            os.position(nal.limit());

            return sh;
        }

        private void copyDataCAVLC(ByteBuffer is, ByteBuffer os, BitReader reader, BitWriter writer) {
            int wLeft = 8 - writer.curBit();
            if (wLeft != 0)
                writer.writeNBit(reader.readNBit(wLeft), wLeft);
            writer.flush();

            // Copy with shift
            int shift = reader.curBit();
            if (shift != 0) {
                int mShift = 8 - shift;
                int inp = reader.readNBit(mShift);
                reader.stop();

                while (is.hasRemaining()) {
                    int out = inp << shift;
                    inp = is.get() & 0xff;
                    out |= inp >> mShift;

                    os.put((byte) out);
                }
                os.put((byte) (inp << shift));
            } else {
                reader.stop();
                os.put(is);
            }
        }

        private void copyDataCABAC(ByteBuffer is, ByteBuffer os, BitReader reader, BitWriter writer) {
            long bp = reader.curBit();
            if (bp != 0) {
                long rem = reader.readNBit(8 - (int) bp);
                if ((1 << (8 - bp)) - 1 != rem)
                    throw new RuntimeException("Invalid CABAC padding");
            }

            if (writer.curBit() != 0)
                writer.writeNBit(0xff, 8 - writer.curBit());
            writer.flush();
            reader.stop();

            os.put(is);
        }
    }

    public static Size getPicSize(SeqParameterSet sps) {
        int w = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int h = getPicHeightInMbs(sps) << 4;
        if (sps.frame_cropping_flag) {
            w -= (sps.frame_crop_left_offset + sps.frame_crop_right_offset) << sps.chroma_format_idc.compWidth[1];
            h -= (sps.frame_crop_top_offset + sps.frame_crop_bottom_offset) << sps.chroma_format_idc.compHeight[1];
        }
        return new Size(w, h);
    }

    public static List<SeqParameterSet> readSPS(List<ByteBuffer> spsList) {
        List<SeqParameterSet> result = new ArrayList<SeqParameterSet>();
        for (ByteBuffer byteBuffer : spsList) {
            result.add(readSPS(NIOUtils.duplicate(byteBuffer)));
        }
        return result;
    }

    public static List<PictureParameterSet> readPPS(List<ByteBuffer> ppsList) {
        List<PictureParameterSet> result = new ArrayList<PictureParameterSet>();
        for (ByteBuffer byteBuffer : ppsList) {
            result.add(readPPS(NIOUtils.duplicate(byteBuffer)));
        }
        return result;
    }

    public static List<ByteBuffer> writePPS(List<PictureParameterSet> allPps) {
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        for (PictureParameterSet pps : allPps) {
            result.add(writePPS(pps, 64));
        }
        return result;
    }

    public static List<ByteBuffer> writeSPS(List<SeqParameterSet> allSps) {
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        for (SeqParameterSet sps : allSps) {
            result.add(writeSPS(sps, 256));
        }
        return result;
    }

    public static void dumpFrame(FileChannelWrapper ch, SeqParameterSet[] values, PictureParameterSet[] values2,
            List<ByteBuffer> nalUnits) throws IOException {
        for (SeqParameterSet sps : values) {
            NIOUtils.writeInt(ch, 1);
            NIOUtils.writeByte(ch, (byte) 0x67);
            ch.write(writeSPS(sps, 128));
        }

        for (PictureParameterSet pps : values2) {
            NIOUtils.writeInt(ch, 1);
            NIOUtils.writeByte(ch, (byte) 0x68);
            ch.write(writePPS(pps, 256));
        }

        for (ByteBuffer byteBuffer : nalUnits) {
            NIOUtils.writeInt(ch, 1);
            ch.write(byteBuffer.duplicate());
        }
    }

    public static void toNAL(ByteBuffer codecPrivate, SeqParameterSet sps, PictureParameterSet pps) {
        ByteBuffer bb1 = ByteBuffer.allocate(512), bb2 = ByteBuffer.allocate(512);
        sps.write(bb1);
        pps.write(bb2);
        bb1.flip();
        bb2.flip();

        putNAL(codecPrivate, bb1, 0x67);
        putNAL(codecPrivate, bb2, 0x68);
    }

    public static void toNAL(ByteBuffer codecPrivate, List<ByteBuffer> spsList2, List<ByteBuffer> ppsList2) {
        for (ByteBuffer byteBuffer : spsList2)
            putNAL(codecPrivate, byteBuffer, 0x67);
        for (ByteBuffer byteBuffer : ppsList2)
            putNAL(codecPrivate, byteBuffer, 0x68);
    }

    private static void putNAL(ByteBuffer codecPrivate, ByteBuffer byteBuffer, int nalType) {
        ByteBuffer dst = ByteBuffer.allocate(byteBuffer.remaining() * 2);
        escapeNAL(byteBuffer, dst);
        dst.flip();
        codecPrivate.putInt(1);
        codecPrivate.put((byte) nalType);
        codecPrivate.put(dst);
    }

    /**
     * Parses a list of SPS NAL units out of the codec private array.
     * @param codecPrivate An AnnexB formatted set of SPS/PPS NAL units.
     * @return A list of ByteBuffers containing PPS NAL units.
     */
    public static List<ByteBuffer> getRawPPS(ByteBuffer codecPrivate) {
        return getRawNALUnitsOfType(codecPrivate, NALUnitType.PPS);
    }

    /**
     * Parses a list of SPS NAL units out of the codec private array.
     * @param codecPrivate An AnnexB formatted set of SPS/PPS NAL units.
     * @return A list of ByteBuffers containing SPS NAL units.
     */
    public static List<ByteBuffer> getRawSPS(ByteBuffer codecPrivate) {
        return getRawNALUnitsOfType(codecPrivate, NALUnitType.SPS);
    }
    
    public static List<ByteBuffer> getRawNALUnitsOfType(ByteBuffer codecPrivate, NALUnitType type) {
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        for (ByteBuffer bb : splitFrame(codecPrivate.duplicate())) {
            NALUnit nu = NALUnit.read(bb);
            if(nu.type == type) {
                result.add(bb);
            }
        }
        return result;
    }
}