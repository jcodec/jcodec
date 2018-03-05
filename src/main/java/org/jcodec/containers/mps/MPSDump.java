package org.jcodec.containers.mps;
import static java.util.Arrays.asList;
import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.codecs.mpeg12.MPEGUtil;
import org.jcodec.codecs.mpeg12.bitstream.CopyrightExtension;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureCodingExtension;
import org.jcodec.codecs.mpeg12.bitstream.PictureDisplayExtension;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureSpatialScalableExtension;
import org.jcodec.codecs.mpeg12.bitstream.PictureTemporalScalableExtension;
import org.jcodec.codecs.mpeg12.bitstream.QuantMatrixExtension;
import org.jcodec.codecs.mpeg12.bitstream.SequenceDisplayExtension;
import org.jcodec.codecs.mpeg12.bitstream.SequenceExtension;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceScalableExtension;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Dumps MPEG Proram stream file. Can firther parse MPEG elementary stream
 * packets.
 * 
 * @author The JCodec project
 * 
 */
public class MPSDump {
    private static final Flag DUMP_FROM = Flag.flag("dump-from", null, "Stop reading at timestamp");
    private static final Flag STOP_AT = Flag.flag("stop-at", null, "Start dumping from timestamp");
    private static final Flag[] ALL_FLAGS = new Flag[] {DUMP_FROM, STOP_AT};
    
    protected ReadableByteChannel ch;

    public MPSDump(ReadableByteChannel ch) {
        this.ch = ch;
    }

    public static void main1(String[] args) throws IOException {
        FileChannelWrapper ch = null;
        try {
            Cmd cmd = MainUtils.parseArguments(args, ALL_FLAGS);
            if (cmd.args.length < 1) {
                MainUtils.printHelp(ALL_FLAGS, asList("file name"));
                return;
            }

            ch = NIOUtils.readableChannel(new File(cmd.args[0]));
            Long dumpAfterPts = cmd.getLongFlag(DUMP_FROM);
            Long stopPts = cmd.getLongFlag(STOP_AT);

            new MPSDump(ch).dump(dumpAfterPts, stopPts);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    public void dump(Long dumpAfterPts, Long stopPts) throws IOException {
        MPEGVideoAnalyzer analyzer = null;
        ByteBuffer buffer = ByteBuffer.allocate(0x100000);

        PESPacket pkt = null;
        int hdrSize = 0;
        for (long position = 0;;) {
            position -= buffer.position();
            if(fillBuffer(buffer) == -1)
                break;
            buffer.flip();
            if (buffer.remaining() < 4)
                break;
            position += buffer.remaining();

            while (true) {
                ByteBuffer payload = null;
                if (pkt != null && pkt.length > 0) {
                    int pesLen = pkt.length - hdrSize + 6;
                    if (pesLen <= buffer.remaining())
                        payload = NIOUtils.read(buffer, pesLen);
                } else {
                    payload = getPesPayload(buffer);
                }
                if (payload == null)
                    break;
                if (pkt != null)
                    logPes(pkt, hdrSize, payload);
                if (analyzer != null && pkt != null && pkt.streamId >= 0xe0 && pkt.streamId <= 0xef) {
                    analyzer.analyzeMpegVideoPacket(payload);
                }
                if (buffer.remaining() < 32) {
                    pkt = null;
                    break;
                }

                skipToNextPES(buffer);

                if (buffer.remaining() < 32) {
                    pkt = null;
                    break;
                }

                hdrSize = buffer.position();
                pkt = readPESHeader(buffer, position - buffer.remaining());
                hdrSize = buffer.position() - hdrSize;
                if (dumpAfterPts != null && pkt.pts >= dumpAfterPts)
                    analyzer = new MPEGVideoAnalyzer();
                if (stopPts != null && pkt.pts >= stopPts)
                    return;
            }
            buffer = transferRemainder(buffer);
        }
    }

    protected int fillBuffer(ByteBuffer buffer) throws IOException {
        return ch.read(buffer);
    }

    protected void logPes(PESPacket pkt, int hdrSize, ByteBuffer payload) {
        System.out.println(pkt.streamId + "(" + (pkt.streamId >= 0xe0 ? "video" : "audio") + ")" + " ["
                + pkt.pos + ", " + (payload.remaining() + hdrSize) + "], pts: " + pkt.pts + ", dts: "
                + pkt.dts);
    }

    private ByteBuffer transferRemainder(ByteBuffer buffer) {
        ByteBuffer dup = buffer.duplicate();
        dup.clear();
        while (buffer.hasRemaining())
            dup.put(buffer.get());
        return dup;
    }

    private static void skipToNextPES(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            int marker = buffer.duplicate().getInt();
            if (marker >= 0x1bd && marker <= 0x1ff && marker != 0x1be)
                break;
            buffer.getInt();
            MPEGUtil.gotoNextMarker(buffer);
        }
    }

    private static ByteBuffer getPesPayload(ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        ByteBuffer result = buffer.duplicate();
        while (copy.hasRemaining()) {
            int marker = copy.duplicate().getInt();
            if (marker >= 0x1b9) {
                result.limit(copy.position());
                buffer.position(copy.position());
                return result;
            }
            copy.getInt();
            MPEGUtil.gotoNextMarker(copy);
        }
        return null;
    }

    private static class MPEGVideoAnalyzer {
        private int nextStartCode = 0xffffffff;
        private ByteBuffer bselPayload;
        private int bselStartCode;
        private int bselOffset;
        private int bselBufInd;
        private int prevBufSize;
        private int curBufInd;
        private PictureHeader picHeader;
        private SequenceHeader sequenceHeader;
        private PictureCodingExtension pictureCodingExtension;
        private SequenceExtension sequenceExtension;
        
        public MPEGVideoAnalyzer() {
            this.bselPayload = ByteBuffer.allocate(0x100000);
        }
        
        private void analyzeMpegVideoPacket(ByteBuffer buffer) {
            int pos = buffer.position();
            int bufSize = buffer.remaining();
            while (buffer.hasRemaining()) {
                bselPayload.put((byte) (nextStartCode >> 24));
                nextStartCode = (nextStartCode << 8) | (buffer.get() & 0xff);
                if (nextStartCode >= 0x100 && nextStartCode <= 0x1b8) {
                    bselPayload.flip();
                    bselPayload.getInt();
                    if (bselStartCode != 0) {
                        if (bselBufInd != curBufInd)
                            bselOffset -= prevBufSize;
                        dumpBSEl(bselStartCode, bselOffset, bselPayload);
                    }
                    bselPayload.clear();
                    bselStartCode = nextStartCode;
                    bselOffset = buffer.position() - 4 - pos;
                    bselBufInd = curBufInd;
                }
            }
            ++curBufInd;
            prevBufSize = bufSize;
        }

        private void dumpBSEl(int mark, int offset, ByteBuffer b) {
            System.out.print(String.format("marker: 0x%02x [@%d] ( ", mark, offset));
            if (mark == 0x100)
                dumpPictureHeader(b);
            else if (mark <= 0x1af)
                System.out.print(MainUtils.colorBright(String.format("slice @0x%02x", mark - 0x101),
                        MainUtils.ANSIColor.BLACK, true));
            else if (mark == 0x1b3)
                dumpSequenceHeader(b);
            else if (mark == 0x1b5)
                dumpExtension(b);
            else if (mark == 0x1b8)
                dumpGroupHeader(b);
            else
                System.out.print("--");

            System.out.println(" )");
        }

        private void dumpExtension(ByteBuffer b) {
            BitReader _in = BitReader.createBitReader(b);
            int extType = _in.readNBit(4);
            if (picHeader == null) {
                if (sequenceHeader != null) {
                    switch (extType) {
                    case SequenceExtension.Sequence_Extension:
                        sequenceExtension = SequenceExtension.read(_in);
                        dumpSequenceExtension(sequenceExtension);
                        break;
                    case SequenceScalableExtension.Sequence_Scalable_Extension:
                        dumpSequenceScalableExtension(SequenceScalableExtension.read(_in));
                        break;
                    case SequenceDisplayExtension.Sequence_Display_Extension:
                        dumpSequenceDisplayExtension(SequenceDisplayExtension.read(_in));
                        break;
                    default:
                        System.out.print(MainUtils.colorBright("extension " + extType, MainUtils.ANSIColor.GREEN, true));
                    }
                } else {
                    System.out.print(MainUtils.colorBright("dangling extension " + extType, MainUtils.ANSIColor.GREEN, true));
                }
            } else {
                switch (extType) {
                case QuantMatrixExtension.Quant_Matrix_Extension:
                    dumpQuantMatrixExtension(QuantMatrixExtension.read(_in));
                    break;
                case CopyrightExtension.Copyright_Extension:
                    dumpCopyrightExtension(CopyrightExtension.read(_in));
                    break;
                case PictureDisplayExtension.Picture_Display_Extension:
                    if (sequenceHeader != null && pictureCodingExtension != null)
                        dumpPictureDisplayExtension(PictureDisplayExtension.read(_in, sequenceExtension,
                                pictureCodingExtension));
                    break;
                case PictureCodingExtension.Picture_Coding_Extension:
                    pictureCodingExtension = PictureCodingExtension.read(_in);
                    dumpPictureCodingExtension(pictureCodingExtension);
                    break;
                case PictureSpatialScalableExtension.Picture_Spatial_Scalable_Extension:
                    dumpPictureSpatialScalableExtension(PictureSpatialScalableExtension.read(_in));
                    break;
                case PictureTemporalScalableExtension.Picture_Temporal_Scalable_Extension:
                    dumpPictureTemporalScalableExtension(PictureTemporalScalableExtension.read(_in));
                    break;
                default:
                    System.out.print(MainUtils.colorBright("extension " + extType, MainUtils.ANSIColor.GREEN, true));
                }
            }
        }

        private void dumpSequenceDisplayExtension(SequenceDisplayExtension read) {
            System.out.print(MainUtils.colorBright("sequence display extension " + dumpBin(read), MainUtils.ANSIColor.GREEN,
                    true));
        }

        private void dumpSequenceScalableExtension(SequenceScalableExtension read) {
            System.out.print(MainUtils.colorBright("sequence scalable extension " + dumpBin(read), MainUtils.ANSIColor.GREEN,
                    true));
        }

        private void dumpSequenceExtension(SequenceExtension read) {
            System.out.print(MainUtils.colorBright("sequence extension " + dumpBin(read), MainUtils.ANSIColor.GREEN, true));
        }

        private void dumpPictureTemporalScalableExtension(PictureTemporalScalableExtension read) {
            System.out.print(MainUtils.colorBright("picture temporal scalable extension " + dumpBin(read),
                    MainUtils.ANSIColor.GREEN, true));
        }

        private void dumpPictureSpatialScalableExtension(PictureSpatialScalableExtension read) {
            System.out.print(MainUtils.colorBright("picture spatial scalable extension " + dumpBin(read),
                    MainUtils.ANSIColor.GREEN, true));
        }

        private void dumpPictureCodingExtension(PictureCodingExtension read) {
            System.out.print(MainUtils.colorBright("picture coding extension " + dumpBin(read), MainUtils.ANSIColor.GREEN,
                    true));
        }

        private void dumpPictureDisplayExtension(PictureDisplayExtension read) {
            System.out.print(MainUtils.colorBright("picture display extension " + dumpBin(read), MainUtils.ANSIColor.GREEN,
                    true));
        }

        private void dumpCopyrightExtension(CopyrightExtension read) {
            System.out.print(MainUtils.colorBright("copyright extension " + dumpBin(read), MainUtils.ANSIColor.GREEN, true));
        }

        private void dumpQuantMatrixExtension(QuantMatrixExtension read) {
            System.out.print(MainUtils
                    .colorBright("quant matrix extension " + dumpBin(read), MainUtils.ANSIColor.GREEN, true));
        }

        private String dumpBin(Object read) {
            StringBuilder bldr = new StringBuilder();
            bldr.append("<");
            Field[] fields = Platform.getFields(read.getClass());
            for (int i = 0; i < fields.length; i++) {
                if (!Modifier.isPublic(fields[i].getModifiers()) || Modifier.isStatic(fields[i].getModifiers()))
                    continue;
                bldr.append(convertName(fields[i].getName()) + ": ");
                if (fields[i].getType().isPrimitive()) {
                    try {
                        bldr.append(fields[i].get(read));
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        Object val = fields[i].get(read);
                        if (val != null)
                            bldr.append(dumpBin(val));
                        else
                            bldr.append("N/A");
                    } catch (Exception e) {
                    }
                }
                if (i < fields.length - 1)
                    bldr.append(",");
            }
            bldr.append(">");
            return bldr.toString();
        }

        private String convertName(String name) {
            return name.replaceAll("([A-Z])", " $1").replaceFirst("^ ", "").toLowerCase();
        }

        private void dumpGroupHeader(ByteBuffer b) {
            GOPHeader gopHeader = GOPHeader.read(b);
            System.out.print(MainUtils.colorBright("group header" + " <closed:" + gopHeader.isClosedGop() + ",broken link:"
                    + gopHeader.isBrokenLink()
                    + (gopHeader.getTimeCode() != null ? (",timecode:" + gopHeader.getTimeCode().toString()) : "")
                    + ">", MainUtils.ANSIColor.MAGENTA, true));
        }

        private void dumpSequenceHeader(ByteBuffer b) {
            picHeader = null;
            pictureCodingExtension = null;
            sequenceExtension = null;
            sequenceHeader = SequenceHeader.read(b);
            System.out.print(MainUtils.colorBright("sequence header", MainUtils.ANSIColor.BLUE, true));
        }

        private void dumpPictureHeader(ByteBuffer b) {
            picHeader = PictureHeader.read(b);
            pictureCodingExtension = null;
            System.out.print(MainUtils.colorBright("picture header" + " <type:"
                    + (picHeader.picture_coding_type == 1 ? "I" : (picHeader.picture_coding_type == 2 ? "P" : "B"))
                    + ", temp_ref:" + picHeader.temporal_reference + ">", MainUtils.ANSIColor.BROWN, true));
        }
    }
}
