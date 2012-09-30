package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.ReaderBE;
import org.jcodec.common.io.WriterBE;
import org.jcodec.common.tools.ToJSON;

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

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        version = (short) ReaderBE.readInt16(input);
        revision = (short) ReaderBE.readInt16(input);
        vendor = ReaderBE.readString(input, 4);
        temporalQual = (int) ReaderBE.readInt32(input);
        spacialQual = (int) ReaderBE.readInt32(input);

        width = (short)ReaderBE.readInt16(input);
        height = (short)ReaderBE.readInt16(input);

        hRes = (float) ReaderBE.readInt32(input) / 65536f;
        vRes = (float) ReaderBE.readInt32(input) / 65536f;

        ReaderBE.readInt32(input); // Reserved

        frameCount = (short) ReaderBE.readInt16(input);

        compressorName = ReaderBE.readPascalString(input, 31);

        depth = (short) ReaderBE.readInt16(input);

        clrTbl = (short) ReaderBE.readInt16(input);

        parseExtensions(input);
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {

        super.doWrite(out);

        out.writeShort(version);
        out.writeShort(revision);
        out.write(vendor.getBytes(), 0, 4);
        out.writeInt(temporalQual);
        out.writeInt(spacialQual);

        out.writeShort((short) width);
        out.writeShort((short) height);

        out.writeInt((int) (hRes * 65536));
        out.writeInt((int) (vRes * 65536));

        out.writeInt(0); // data size

        out.writeShort(frameCount);

        WriterBE.writePascalString(out, compressorName, 31);

        out.writeShort(depth);

        out.writeShort(clrTbl);

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

    public static class MyFactory extends BoxFactory {
        private Map<String, Class<? extends Box>> mappings = new HashMap<String, Class<? extends Box>>();

        public MyFactory() {
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

    @Override
    public void dump(StringBuilder sb) {
        sb.append(header.getFourcc() + ": {\n");
        sb.append("entry: ");
        ToJSON.toJSON(this, sb, "version", "revision", "vendor", "temporalQual", "spacialQual", "width", "height",
                "hRes", "vRes", "frameCount", "compressorName", "depth", "clrTbl");
        sb.append(",\nexts: [\n");
        dumpBoxes(sb);
        sb.append("\n]\n");
        sb.append("}\n");
    }
}