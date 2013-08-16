package org.jcodec.codecs.h264;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
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
 * @author Jay Codec
 * 
 */
public class H264Utils {

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
        ByteBuffer in = _buf.duplicate();
        ByteBuffer out = _buf.duplicate();
        byte p1 = in.get();
        out.put(p1);
        byte p2 = in.get();
        out.put(p2);
        while (in.hasRemaining()) {
            byte b = in.get();
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
        while (dup.hasRemaining()) {
            int len = readLen(dup, nls);
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
     * @param spsList
     *            Storage for leading SPS structures ( can be null, then all
     *            leading SPSs are discarded ).
     * @param ppsList
     *            Storage for leading PPS structures ( can be null, then all
     *            leading PPSs are discarded ).
     */
    public static void encodeMOVPacket(ByteBuffer avcFrame, List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {

        ByteBuffer dup = avcFrame.duplicate();
        ByteBuffer d1 = avcFrame.duplicate();

        for (int tot = 0;;) {
            ByteBuffer buf = H264Utils.nextNALUnit(dup);
            if (buf == null)
                break;
            d1.position(tot);
            d1.putInt(buf.remaining());
            tot += buf.remaining() + 4;

            NALUnit nu = NALUnit.read(buf);

            if (nu.type == NALUnitType.PPS && ppsList != null) {
                ppsList.add(buf);
            } else if (nu.type == NALUnitType.SPS && spsList != null) {
                spsList.add(buf);
            }
        }
    }

    public static SampleEntry createMOVSampleEntry(List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {
        SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
        AvcCBox avcC = new AvcCBox(sps.profile_idc, 0, sps.level_idc, spsList, ppsList);

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

    public static SampleEntry createMOVSampleEntry(SeqParameterSet initSPS, PictureParameterSet initPPS) {
        ByteBuffer bb1 = ByteBuffer.allocate(512), bb2 = ByteBuffer.allocate(512);
        initSPS.write(bb1);
        initPPS.write(bb2);
        bb1.flip();
        bb2.flip();
        return createMOVSampleEntry(Arrays.asList(new ByteBuffer[] { bb1 }), Arrays.asList(new ByteBuffer[] { bb2 }));
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
        SeekableByteChannel raw = NIOUtils.writableFileChannel(f);
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

    public static void joinNALUnits(List<ByteBuffer> nalUnits, ByteBuffer out) {
        for (ByteBuffer nal : nalUnits) {
            out.putInt(1);
            out.put(nal.duplicate());
        }
    }

    public static AvcCBox parseAVCC(VideoSampleEntry vse) {
        LeafBox lb = Box.findFirst(vse, LeafBox.class, "avcC");
        AvcCBox avcC = new AvcCBox();
        avcC.parse(lb.getData().duplicate());
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
}