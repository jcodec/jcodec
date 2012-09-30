package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.io.ReaderBE.readInt16;
import static org.jcodec.common.io.ReaderBE.readInt32;
import static org.jcodec.common.io.ReaderBE.readPascalString;
import static org.jcodec.common.io.ReaderBE.readString;
import static org.jcodec.common.io.ReaderBE.sureRead;
import static org.jcodec.common.io.WriterBE.writePascalString;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.wav.StringReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AliasBox extends FullBox {
    public final static int DirectoryName = 0;
    public final static int DirectoryIDs = 1; // parent & higher directory ids
                                              // '/' is not counted, one
                                              // unsigned32 for each dir
    public final static int AbsolutePath = 2;
    public final static int AppleShareZoneName = 3;
    public final static int AppleShareServerName = 4;
    public final static int AppleShareUserName = 5;
    public final static int DriverName = 6;
    public final static int RevisedAppleShare = 9;
    public final static int AppleRemoteAccessDialup = 10;
    public final static int UNIXAbsolutePath = 18;
    public final static int UTF16AbsolutePath = 14;
    public final static int UFT16VolumeName = 15; // 26
    public final static int VolumeMountPoint = 19; // 1

    private String type;
    private short recordSize;
    private short version;
    private short kind;
    private String volumeName;
    private int volumeCreateDate;
    private short volumeSignature;
    private short volumeType;
    private int parentDirId;
    private String fileName;
    private int fileNumber;
    private int createdLocalDate;
    private String fileTypeName;
    private String creatorName;
    private short nlvlFrom;
    private short nlvlTo;
    private int volumeAttributes;
    private short fsId;
    private List<ExtraField> extra;

    public static String fourcc() {
        return "alis";
    }

    private static Set<Integer> utf16 = new HashSet<Integer>();
    static {
        utf16.add(14);
        utf16.add(15);
    }

    public static class ExtraField {
        short type;
        int len;
        byte[] data;

        public ExtraField(short type, int len, byte[] bs) {
            this.type = type;
            this.len = len;
            this.data = bs;
        }

        public String toString() {
            try {
                return new String(data, 0, len, utf16.contains(type) ? "UTF-16" : "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            return null;
        }
    }

    public AliasBox() {
        super(new Header(fourcc(), 0));
    }

    public AliasBox(Header atom) {
        super(atom);
    }

    public void parse(InputStream is) throws IOException {
        super.parse(is);
        if ((flags & 0x1) != 0) // self ref
            return;
        type = readString(is, 4);
        recordSize = (short) readInt16(is);
        version = (short) readInt16(is);
        kind = (short) readInt16(is);
        volumeName = readPascalString(is, 27);
        volumeCreateDate = (int) readInt32(is);
        volumeSignature = (short) readInt16(is);
        volumeType = (short) readInt16(is);
        parentDirId = (int) readInt32(is);
        fileName = readPascalString(is, 63);
        fileNumber = (int) readInt32(is);
        createdLocalDate = (int) readInt32(is);
        fileTypeName = readString(is, 4);
        creatorName = readString(is, 4);
        nlvlFrom = (short) readInt16(is);
        nlvlTo = (short) readInt16(is);
        volumeAttributes = (int) readInt32(is);
        fsId = (short) readInt16(is);
        StringReader.sureSkip(is, 10);

        extra = new ArrayList<ExtraField>();
        while (true) {
            short type = (short) readInt16(is);
            if (type == -1)
                break;
            int len = (int) readInt16(is);
            byte[] bs = sureRead(is, (len + 1) & 0xfffffffe);
            if (bs == null)
                break;
            extra.add(new ExtraField(type, len, bs));
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        if ((flags & 0x1) != 0) // self ref
            return;
        out.write(type.getBytes(), 0, 4);
        out.writeShort(recordSize);
        out.writeShort(version);
        out.writeShort(kind);
        writePascalString(out, volumeName, 27);
        out.writeInt(volumeCreateDate);
        out.writeShort(volumeSignature);
        out.writeShort(volumeType);
        out.writeInt(parentDirId);
        writePascalString(out, fileName, 63);
        out.writeInt(fileNumber);
        out.writeInt(createdLocalDate);
        out.write(fileTypeName.getBytes(), 0, 4);
        out.write(creatorName.getBytes(), 0, 4);
        out.writeShort(nlvlFrom);
        out.writeShort(nlvlTo);
        out.writeInt(volumeAttributes);
        out.writeShort(fsId);
        out.write(new byte[10]);
        for (ExtraField extraField : extra) {
            out.writeShort(extraField.type);
            out.writeShort(extraField.len);
            out.write(extraField.data);
        }
        out.writeShort(-1);
        out.writeShort(0);
    }

    public int getRecordSize() {
        return recordSize;
    }

    public String getFileName() {
        return fileName;
    }

    public List<ExtraField> getExtra() {
        return extra;
    }

    public ExtraField getExtra(int type) {
        for (ExtraField extraField : extra) {
            if (extraField.type == type)
                return extraField;
        }
        return null;
    }

    public boolean isSelfRef() {
        return (flags & 0x1) != 0;
    }

    public static AliasBox createSelfRef() {
        AliasBox alis = new AliasBox();
        alis.setFlags(1);
        return alis;
    }

    @Override
    protected void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": ");
        if (isSelfRef())
            sb.append("'self'");
        else
            sb.append("'" + getUnixPath() + "'");

    }

    public String getUnixPath() {
        ExtraField extraField = getExtra(AliasBox.UNIXAbsolutePath);
        return extraField == null ? null : "/" + extraField.toString();
    }
}
