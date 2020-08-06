package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.JCodecUtil2.asciiString;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.boxes.Box.AtomField;
import org.jcodec.platform.Platform;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
            return Platform.stringFromCharset4(data, 0, len, (type == 14 || type == 15) ? Platform.UTF_16 : Platform.UTF_8);
        }

        @AtomField(idx=0)
        public short getType() {
            return type;
        }
        @AtomField(idx=1)
        public int getLen() {
            return len;
        }
        @AtomField(idx=2)
        public byte[] getData() {
            return data;
        }
        
    }

    public AliasBox(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer is) {
        super.parse(is);
        if ((flags & 0x1) != 0) // self ref
            return;
        type = NIOUtils.readString(is, 4);
        recordSize = is.getShort();
        version = is.getShort();
        kind = is.getShort();
        volumeName = NIOUtils.readPascalStringL(is, 27);
        volumeCreateDate = is.getInt();
        volumeSignature = is.getShort();
        volumeType = is.getShort();
        parentDirId = is.getInt();
        fileName = NIOUtils.readPascalStringL(is, 63);
        fileNumber = is.getInt();
        createdLocalDate = is.getInt();
        fileTypeName = NIOUtils.readString(is, 4);
        creatorName = NIOUtils.readString(is, 4);
        nlvlFrom = is.getShort();
        nlvlTo = is.getShort();
        volumeAttributes = is.getInt();
        fsId = is.getShort();
        NIOUtils.skip(is, 10);

        extra = new ArrayList<ExtraField>();
        while (true) {
            short type = is.getShort();
            if (type == -1)
                break;
            int len = is.getShort();
            byte[] bs = NIOUtils.toArray(NIOUtils.read(is, (len + 1) & 0xfffffffe));
            if (bs == null)
                break;
            extra.add(new ExtraField(type, len, bs));
        }
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        if ((flags & 0x1) != 0) // self ref
            return;
        out.put(asciiString(type), 0, 4);
        out.putShort(recordSize);
        out.putShort(version);
        out.putShort(kind);
        NIOUtils.writePascalStringL(out, volumeName, 27);
        out.putInt(volumeCreateDate);
        out.putShort(volumeSignature);
        out.putShort(volumeType);
        out.putInt(parentDirId);
        NIOUtils.writePascalStringL(out, fileName, 63);
        out.putInt(fileNumber);
        out.putInt(createdLocalDate);
        out.put(asciiString(fileTypeName), 0, 4);
        out.put(asciiString(creatorName), 0, 4);
        out.putShort(nlvlFrom);
        out.putShort(nlvlTo);
        out.putInt(volumeAttributes);
        out.putShort(fsId);
        out.put(new byte[10]);
        for (ExtraField extraField : extra) {
            out.putShort(extraField.type);
            out.putShort((short) extraField.len);
            out.put(extraField.data);
        }
        out.putShort((short) -1);
        out.putShort((short) 0);
    }
    
    @Override
    public int estimateSize() {
        int sz = 166;
        if ((flags & 0x1) == 0) {
            for (ExtraField extraField : extra) {
                sz += 4 + extraField.data.length;
            }
        }
        return 12 + sz;
    }

    @AtomField(idx=1)
    public int getRecordSize() {
        return recordSize;
    }

    @AtomField(idx=8)
    public String getFileName() {
        return fileName;
    }

    public List<ExtraField> getExtras() {
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
        AliasBox alis = new AliasBox(new Header(fourcc()));
        alis.setFlags(1);
        return alis;
    }

    public String getUnixPath() {
        ExtraField extraField = getExtra(AliasBox.UNIXAbsolutePath);
        return extraField == null ? null : "/" + extraField.toString();
    }

    @AtomField(idx=0)
    public String getType() {
        return type;
    }

    @AtomField(idx=2)
    public short getKind() {
        return kind;
    }

    @AtomField(idx=3)
    public String getVolumeName() {
        return volumeName;
    }

    @AtomField(idx=4)
    public int getVolumeCreateDate() {
        return volumeCreateDate;
    }

    @AtomField(idx=5)
    public short getVolumeSignature() {
        return volumeSignature;
    }

    @AtomField(idx=6)
    public short getVolumeType() {
        return volumeType;
    }

    @AtomField(idx=7)
    public int getParentDirId() {
        return parentDirId;
    }

    @AtomField(idx=9)
    public int getFileNumber() {
        return fileNumber;
    }

    @AtomField(idx=10)
    public int getCreatedLocalDate() {
        return createdLocalDate;
    }

    @AtomField(idx=11)
    public String getFileTypeName() {
        return fileTypeName;
    }

    @AtomField(idx=12)
    public String getCreatorName() {
        return creatorName;
    }

    @AtomField(idx=13)
    public short getNlvlFrom() {
        return nlvlFrom;
    }

    @AtomField(idx=14)
    public short getNlvlTo() {
        return nlvlTo;
    }

    @AtomField(idx=15)
    public int getVolumeAttributes() {
        return volumeAttributes;
    }

    @AtomField(idx=16)
    public short getFsId() {
        return fsId;
    }

    @AtomField(idx=17)
    public List<ExtraField> getExtra() {
        return extra;
    }
}
