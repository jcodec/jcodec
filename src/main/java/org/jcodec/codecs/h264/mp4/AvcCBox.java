package org.jcodec.codecs.h264.mp4;

import org.jcodec.common.Assert;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public class AvcCBox extends Box {

    private int profile;
    private int profileCompat;
    private int level;
    private int nalLengthSize;

    private List<ByteBuffer> spsList;
    private List<ByteBuffer> ppsList;

    public AvcCBox(Header header) {
        super(header);
        this.spsList = new ArrayList<ByteBuffer>();
        this.ppsList = new ArrayList<ByteBuffer>();
    }

    public static String fourcc() {
        return "avcC";
    }

    public static AvcCBox parseAvcCBox(ByteBuffer buf) {
        AvcCBox avcCBox = new AvcCBox(new Header(fourcc()));
        avcCBox.parse(buf);
        return avcCBox;
    }

    public static AvcCBox createEmpty() {
        return new AvcCBox(new Header(fourcc()));
    }
    
    public static AvcCBox createAvcCBox(int profile, int profileCompat, int level, int nalLengthSize,
            List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {
        AvcCBox avcc = new AvcCBox(new Header(fourcc()));
        avcc.profile = profile;
        avcc.profileCompat = profileCompat;
        avcc.level = level;
        avcc.nalLengthSize = nalLengthSize;
        avcc.spsList = spsList;
        avcc.ppsList = ppsList;
        return avcc;
    }

    @Override
    public void parse(ByteBuffer input) {
        NIOUtils.skip(input, 1);
        profile = input.get() & 0xff;
        profileCompat = input.get() & 0xff;
        level = input.get() & 0xff;
        int flags = input.get() & 0xff;
        nalLengthSize = (flags & 0x03) + 1;

        int nSPS = input.get() & 0x1f; // 3 bits reserved + 5 bits number of
                                       // sps
        for (int i = 0; i < nSPS; i++) {
            int spsSize = input.getShort();
            Assert.assertEquals(0x27, input.get() & 0x3f);
            spsList.add(NIOUtils.read(input, spsSize - 1));
        }

        int nPPS = input.get() & 0xff;
        for (int i = 0; i < nPPS; i++) {
            int ppsSize = input.getShort();
            Assert.assertEquals(0x28, input.get() & 0x3f);
            ppsList.add(NIOUtils.read(input, ppsSize - 1));
        }
    }

    @Override
    public void doWrite(ByteBuffer out) {
        out.put((byte) 0x1); // version
        out.put((byte) profile);
        out.put((byte) profileCompat);
        out.put((byte) level);
        out.put((byte) 0xff);

        out.put((byte) (spsList.size() | 0xe0));
        for (ByteBuffer sps : spsList) {
            out.putShort((short) (sps.remaining() + 1));
            out.put((byte) 0x67);
            NIOUtils.write(out, sps);
        }

        out.put((byte) ppsList.size());
        for (ByteBuffer pps : ppsList) {
            out.putShort((byte) (pps.remaining() + 1));
            out.put((byte) 0x68);
            NIOUtils.write(out, pps);
        }
    }
    
    @Override
    public int estimateSize() {
        int sz = 17;
        for (ByteBuffer sps : spsList) {
            sz += 3 + sps.remaining();
        }

        for (ByteBuffer pps : ppsList) {
            sz += 3 + pps.remaining();
        }
        return sz;
    }

    public int getProfile() {
        return profile;
    }

    public int getProfileCompat() {
        return profileCompat;
    }

    public int getLevel() {
        return level;
    }

    public List<ByteBuffer> getSpsList() {
        return spsList;
    }

    public List<ByteBuffer> getPpsList() {
        return ppsList;
    }

    public int getNalLengthSize() {
        return nalLengthSize;
    }

    //    public void toNAL(ByteBuffer codecPrivate) {
    //        H264Utils.toNAL(codecPrivate, getSpsList(), getPpsList());
    //    }
    //    
    //    public ByteBuffer toNAL() {
    //        ByteBuffer bb = ByteBuffer.allocate(2048);
    //        H264Utils.toNAL(bb, getSpsList(), getPpsList());
    //        bb.flip();
    //        return bb;
    //    }
    //
    //    public static AvcCBox fromNAL(ByteBuffer codecPrivate) {
    //        List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
    //        List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
    //
    //        ByteBuffer dup = codecPrivate.duplicate();
    //
    //        ByteBuffer buf;
    //        SeqParameterSet sps = null;
    //        while ((buf = H264Utils.nextNALUnit(dup)) != null) {
    //            NALUnit nu = NALUnit.read(buf);
    //            
    //            H264Utils.unescapeNAL(buf);
    //            
    //            if (nu.type == NALUnitType.PPS) {
    //                ppsList.add(buf);
    //            } else if (nu.type == NALUnitType.SPS) {
    //                spsList.add(buf);
    //                sps = SeqParameterSet.read(buf.duplicate());
    //            }
    //        }
    //        if (spsList.size() == 0 || ppsList.size() == 0)
    //            return null;
    //        return new AvcCBox(sps.profile_idc, 0, sps.level_idc, spsList, ppsList);
    //    }
}