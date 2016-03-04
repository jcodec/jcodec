package org.jcodec.containers.mp4;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AvcCUtil {

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
     * Joins buffers containing individual NAL units into a single AnnexB delimited buffer.
     * Each NAL unit will be separated with 00 00 00 01 markers. Allocates a new byte buffer
     * and writes data into it.
     * @param nalUnits
     */
    public static ByteBuffer joinNALUnits(List<ByteBuffer> nalUnits) {
        int size = 0;
        for (ByteBuffer nal : nalUnits) {
            size += 4 + nal.remaining();
        }
        ByteBuffer allocate = ByteBuffer.allocate(size);
        joinNALUnitsToBuffer(nalUnits, allocate);
        return allocate;
    }

    /**
     * Joins buffers containing individual NAL units into a single AnnexB delimited buffer.
     * Each NAL unit will be separated with 00 00 00 01 markers.
     * @param nalUnits
     * @param out
     */
    public static void joinNALUnitsToBuffer(List<ByteBuffer> nalUnits, ByteBuffer out) {
        for (ByteBuffer nal : nalUnits) {
            out.putInt(1);
            out.put(nal.duplicate());
        }
    }

    /**
     * Decodes AVC packet in ISO BMF format into Annex B format.
     * <p/>
     * Replaces NAL unit size integers with 00 00 00 01 start codes. If the
     * space allows the transformation is done inplace.
     *
     * @param result
     */
    public static ByteBuffer decodeMOVPacket(ByteBuffer result, AvcCBox avcC) {
        if (avcC.getNalLengthSize() == 4) {
            decodeMOVPacketInplace(result, avcC);
            return result;
        }
        return joinNALUnits(splitMOVPacket(result, avcC));
    }

    /**
     * Decodes AVC packet in ISO BMF format into Annex B format.
     * <p/>
     * Inplace replaces NAL unit size integers with 00 00 00 01 start codes.
     *
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

    public static AvcCBox createAvcC(SeqParameterSet sps, PictureParameterSet pps, int nalLengthSize) {
        ByteBuffer serialSps = ByteBuffer.allocate(512);
        sps.write(serialSps);
        serialSps.flip();
        H264Utils.escapeNALinplace(serialSps);

        ByteBuffer serialPps = ByteBuffer.allocate(512);
        pps.write(serialPps);
        serialPps.flip();
        H264Utils.escapeNALinplace(serialPps);

        AvcCBox avcC = AvcCBox.createAvcCBox(sps.profile_idc, 0, sps.level_idc, nalLengthSize, asList(serialSps), asList(serialPps));

        return avcC;
    }

    public static AvcCBox createAvcCFromList(List<SeqParameterSet> initSPS, List<PictureParameterSet> initPPS, int nalLengthSize) {
        List<ByteBuffer> serialSps = H264Utils.saveSPS(initSPS);
        List<ByteBuffer> serialPps = H264Utils.savePPS(initPPS);

        SeqParameterSet sps = initSPS.get(0);
        return AvcCBox.createAvcCBox(sps.profile_idc, 0, sps.level_idc, nalLengthSize, serialSps, serialPps);
    }

    /**
     * Creates a MP4 sample entry given AVC/H.264 codec private.
     * @param codecPrivate Array containing AnnexB delimited (00 00 00 01) SPS/PPS NAL units.
     * @return MP4 sample entry
     */
    public static SampleEntry createMOVSampleEntryFromBytes(byte[] codecPrivate) {
        List<ByteBuffer> rawSPS = H264Utils.getRawSPS(ByteBuffer.wrap(codecPrivate));
        List<ByteBuffer> rawPPS = H264Utils.getRawPPS(ByteBuffer.wrap(codecPrivate));
        return createMOVSampleEntryFromSpsPpsList(rawSPS, rawPPS, 4);
    }

    public static SampleEntry createMOVSampleEntryFromSpsPpsList(List<ByteBuffer> spsList, List<ByteBuffer> ppsList, int nalLengthSize) {
        SeqParameterSet sps = H264Utils.readSPS(NIOUtils.duplicate(spsList.get(0)));
        AvcCBox avcC = AvcCBox.createAvcCBox(sps.profile_idc, 0, sps.level_idc, nalLengthSize, spsList, ppsList);

        return createMOVSampleEntryFromAvcC(avcC);
    }

    public static SampleEntry createMOVSampleEntryFromAvcC(AvcCBox avcC) {
        SeqParameterSet sps = SeqParameterSet.read(avcC.getSpsList().get(0).duplicate());
        int codedWidth = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int codedHeight = SeqParameterSet.getPicHeightInMbs(sps) << 4;

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

    public static SampleEntry createMOVSampleEntryFromSpsPps(SeqParameterSet initSPS, PictureParameterSet initPPS,
                                                             int nalLengthSize) {
        ByteBuffer bb1 = ByteBuffer.allocate(512), bb2 = ByteBuffer.allocate(512);
        initSPS.write(bb1);
        initPPS.write(bb2);
        bb1.flip();
        bb2.flip();
        return createMOVSampleEntryFromBuffer(bb1, bb2, nalLengthSize);
    }

    public static SampleEntry createMOVSampleEntryFromBuffer(ByteBuffer sps, ByteBuffer pps, int nalLengthSize) {
        return createMOVSampleEntryFromSpsPpsList(Arrays.asList(new ByteBuffer[] { sps }), Arrays.asList(new ByteBuffer[] { pps }),
                nalLengthSize);
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
            raw.write(ByteBuffer.wrap(new byte[]{0, 0, 0, 1, 0x67}));

            H264Utils.escapeNAL(byteBuffer.duplicate(), bb);
            bb.flip();
            raw.write(bb);
            bb.clear();
        }
        for (ByteBuffer byteBuffer : avcC.getPpsList()) {
            raw.write(ByteBuffer.wrap(new byte[]{0, 0, 0, 1, 0x68}));
            H264Utils.escapeNAL(byteBuffer.duplicate(), bb);
            bb.flip();
            raw.write(bb);
            bb.clear();
        }
    }

    public static ByteBuffer getAvcCData(AvcCBox avcC) {
        ByteBuffer bb = ByteBuffer.allocate(2048);
        avcC.doWrite(bb);
        bb.flip();
        return bb;
    }

    public static AvcCBox parseAVCC(VideoSampleEntry vse) {
        Box lb = NodeBox.findFirst(vse, Box.class, "avcC");
        if (lb instanceof AvcCBox)
            return (AvcCBox) lb;
        else {
            return parseAVCCFromBuffer(((LeafBox) lb).getData().duplicate());
        }
    }

    public static byte[] avcCToAnnexB(AvcCBox avcC) {
        return H264Utils.saveCodecPrivate(avcC.getSpsList(), avcC.getPpsList());
    }

    public static AvcCBox parseAVCCFromBuffer(ByteBuffer bb) {
        return AvcCBox.parseAvcCBox(bb);
    }
}