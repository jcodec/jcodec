package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;

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
    private static final MyFactory FACTORY = new MyFactory();
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

    public VideoSampleEntry(Header atom, short version, short revision, String vendor, int temporalQual,
            int spacialQual, short width, short height, long hRes, long vRes, short frameCount, String compressorName,
            short depth, short drefInd, short clrTbl) {
        super(atom, drefInd);
        factory = FACTORY;
        this.version = version;
        this.revision = revision;
        this.vendor = vendor;
        this.temporalQual = temporalQual;
        this.spacialQual = spacialQual;
        this.width = width;
        this.height = height;
        this.hRes = hRes;
        this.vRes = vRes;
        this.frameCount = frameCount;
        this.compressorName = compressorName;
        this.depth = depth;
        this.clrTbl = clrTbl;
    }

    public VideoSampleEntry(Header atom) {
        super(atom);
        factory = FACTORY;
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

        compressorName = NIOUtils.readPascalString(input, 31);

        depth = input.getShort();

        clrTbl = input.getShort();

        parseExtensions(input);
    }

    @Override
    public void doWrite(ByteBuffer out) {

        super.doWrite(out);

        out.putShort(version);
        out.putShort(revision);
        out.put(JCodecUtil.asciiString(vendor), 0, 4);
        out.putInt(temporalQual);
        out.putInt(spacialQual);

        out.putShort((short) width);
        out.putShort((short) height);

        out.putInt((int) (hRes * 65536));
        out.putInt((int) (vRes * 65536));

        out.putInt(0); // data size

        out.putShort(frameCount);

        NIOUtils.writePascalString(out, compressorName, 31);

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

    public static class MyFactory extends BoxFactory {
        private Map<String, Class<? extends Box>> mappings;
        
        public MyFactory() {
            this.mappings = new HashMap<String, Class<? extends Box>>();

            mappings.put(PixelAspectExt.fourcc(), PixelAspectExt.class);
            mappings.put(AvcCBox.fourcc(), AvcCBox.class);
            mappings.put(ColorExtension.fourcc(), ColorExtension.class);
            mappings.put(GamaExtension.fourcc(), GamaExtension.class);
            mappings.put(CleanApertureExtension.fourcc(), CleanApertureExtension.class);
            mappings.put(FielExtension.fourcc(), FielExtension.class);
        }

        public Class<? extends Box> toClass(String fourcc) {
            return mappings.get(fourcc);
        }
    }
}