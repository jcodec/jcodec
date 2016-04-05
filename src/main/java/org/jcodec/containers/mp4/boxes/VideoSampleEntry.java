package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.io.NIOUtils;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Describes video payload sample
 * 
 * @author The JCodec project
 * 
 */
public class VideoSampleEntry extends SampleEntry {
    public static VideoSampleEntry createVideoSampleEntry(Header atom, short version, short revision, String vendor,
            int temporalQual, int spacialQual, short width, short height, long hRes, long vRes, short frameCount,
            String compressorName, short depth, short drefInd, short clrTbl) {
        VideoSampleEntry e = new VideoSampleEntry(atom);
        e.drefInd = drefInd;
        e.version = version;
        e.revision = revision;
        e.vendor = vendor;
        e.temporalQual = temporalQual;
        e.spacialQual = spacialQual;
        e.width = width;
        e.height = height;
        e.hRes = hRes;
        e.vRes = vRes;
        e.frameCount = frameCount;
        e.compressorName = compressorName;
        e.depth = depth;
        e.clrTbl = clrTbl;
        return e;
    }

    private short version;
    private short revision;
    private String vendor;
    private int temporalQual;
    private int spacialQual;
    private short width;
    private short height;
    private float hRes;
    private float vRes;
    private short frameCount;
    private String compressorName;
    private short depth;
    private short clrTbl;

    public VideoSampleEntry(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        version = input.getShort();
        revision = input.getShort();
        vendor = NIOUtils.readString(input, 4);
        temporalQual = input.getInt();
        spacialQual = input.getInt();

        width = input.getShort();
        height = input.getShort();

        hRes = (float) input.getInt() / 65536f;
        vRes = (float) input.getInt() / 65536f;

        input.getInt(); // Reserved

        frameCount = input.getShort();

        compressorName = NIOUtils.readPascalStringL(input, 31);

        depth = input.getShort();

        clrTbl = input.getShort();

        parseExtensions(input);
    }

    @Override
    public void doWrite(ByteBuffer out) {

        super.doWrite(out);

        out.putShort(version);
        out.putShort(revision);
        out.put3(JCodecUtil2.asciiString(vendor), 0, 4);
        out.putInt(temporalQual);
        out.putInt(spacialQual);

        out.putShort((short) width);
        out.putShort((short) height);

        out.putInt((int) (hRes * 65536));
        out.putInt((int) (vRes * 65536));

        out.putInt(0); // data size

        out.putShort(frameCount);

        NIOUtils.writePascalStringL(out, compressorName, 31);

        out.putShort(depth);

        out.putShort(clrTbl);

        writeExtensions(out);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float gethRes() {
        return hRes;
    }

    public float getvRes() {
        return vRes;
    }

    public long getFrameCount() {
        return frameCount;
    }

    public String getCompressorName() {
        return compressorName;
    }

    public long getDepth() {
        return depth;
    }

    public String getVendor() {
        return vendor;
    }

    public short getVersion() {
        return version;
    }

    public short getRevision() {
        return revision;
    }

    public int getTemporalQual() {
        return temporalQual;
    }

    public int getSpacialQual() {
        return spacialQual;
    }

    public short getClrTbl() {
        return clrTbl;
    }
}