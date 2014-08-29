package org.jcodec.containers.mps;

import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.jcodec.codecs.mpeg12.MPEGUtil;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.PictureHeader;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

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
    private static final String DUMP_FROM = "dump-from";
    private static final String STOP_AT = "stop-at";

    public static void main(String[] args) throws IOException {
        FileChannelWrapper ch = null;
        try {
            Cmd cmd = MainUtils.parseArguments(args);
            if (cmd.args.length < 1) {
                MainUtils.printHelp(new HashMap<String, String>() {
                    {
                        put(STOP_AT, "Stop reading at timestamp");
                        put(DUMP_FROM, "Start dumping from timestamp");
                    }
                }, "file name");
                return;
            }

            ch = NIOUtils.readableFileChannel(new File(cmd.args[0]));
            Long dumpAfterPts = cmd.getLongFlag(DUMP_FROM);
            Long stopPts = cmd.getLongFlag(STOP_AT);

            MPEGVideoAnalyzer analyzer = null;

            PESPacket pkt = null;
            int hdrSize = 0;
            while (ch.position() + 4 < ch.size()) {
                ByteBuffer buffer = NIOUtils.fetchFrom(ch, 0x100000);

                while (true) {
                    ByteBuffer payload = null;
                    if (pkt != null && pkt.length > 0) {
                        int pesLen = pkt.length - hdrSize + 6;
                        if(pesLen <= buffer.remaining())
                            payload = NIOUtils.read(buffer, pesLen);
                    } else {
                        payload = getPesPayload(buffer);
                    }
                    if(payload == null)
                        break;
                    if (pkt != null)
                        System.out.println(pkt.streamId + "(" + (pkt.streamId >= 0xe0 ? "video" : "audio") + ")" + " ["
                                + pkt.pos + ", " + (payload.remaining() + hdrSize) + "], pts: " + pkt.pts + ", dts: "
                                + pkt.dts);
                    if (analyzer != null && pkt != null && pkt.streamId >= 0xe0 && pkt.streamId <= 0xef) {
                        analyzer.analyzeMpegVideoPacket(payload);
                    }
                    if (buffer.remaining() < 32)
                        break;

                    skipToNextPES(buffer);
                    
                    if (buffer.remaining() < 32)
                        break;

                    hdrSize = buffer.position();
                    pkt = readPESHeader(buffer, ch.position() - buffer.remaining());
                    hdrSize = buffer.position() - hdrSize;
                    if (dumpAfterPts != null && pkt.pts >= dumpAfterPts)
                        analyzer = new MPEGVideoAnalyzer();
                    if (stopPts != null && pkt.pts >= stopPts)
                        return;
                }
                ch.position(ch.position() - buffer.remaining());
            }
        } finally {
            NIOUtils.closeQuietly(ch);
        }
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
            if (marker > 0x1af) {
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
        private ByteBuffer bselPayload = ByteBuffer.allocate(0x100000);
        private int bselStartCode;
        private int bselOffset;
        private int bselBufInd;
        private int prevBufSize;
        private int curBufInd;

        private void analyzeMpegVideoPacket(ByteBuffer buffer) {
            int pos = buffer.position();
            int bufSize = buffer.remaining();
            while (buffer.hasRemaining()) {
                bselPayload.put((byte) (nextStartCode >> 24));
                nextStartCode = (nextStartCode << 8) | (buffer.get() & 0xff);
                if (nextStartCode >= 0x100 && nextStartCode <= 0x1b8) {
                    bselPayload.flip();
                    bselPayload.getInt();
                    if(bselStartCode != 0) {
                        if(bselBufInd != curBufInd)
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

        private static void dumpBSEl(int mark, int offset, ByteBuffer b) {
            System.out.print(String.format("marker: 0x%02x [@%d] ( ", mark, offset));
            if (mark == 0x100)
                dumpPictureHeader(b);
            else if (mark <= 0x1af)
                System.out.print(MainUtils.color(String.format("slice @0x%02x", mark - 0x101),
                        MainUtils.ANSIColor.BLACK, true));
            else if (mark == 0x1b3)
                dumpSequenceHeader();
            else if (mark == 0x1b5)
                dumpExtension();
            else if (mark == 0x1b8)
                dumpGroupHeader(b);
            else
                System.out.print("--");

            System.out.println(" )");
        }

        private static void dumpExtension() {
            System.out.print(MainUtils.color("extension", MainUtils.ANSIColor.GREEN, true));
        }

        private static void dumpGroupHeader(ByteBuffer b) {
            GOPHeader gopHeader = GOPHeader.read(b);
            System.out.print(MainUtils.color("group header" + " <closed:" + gopHeader.isClosedGop() + ",broken link:"
                    + gopHeader.isBrokenLink()
                    + (gopHeader.getTimeCode() != null ? (",timecode:" + gopHeader.getTimeCode().toString()) : "")
                    + ">", MainUtils.ANSIColor.MAGENTA, true));
        }

        private static void dumpSequenceHeader() {
            System.out.print(MainUtils.color("sequence header", MainUtils.ANSIColor.BLUE, true));
        }

        private static void dumpPictureHeader(ByteBuffer b) {
            PictureHeader picHeader = PictureHeader.read(b);
            System.out.print(MainUtils.color("picture header" + " <type:"
                    + (picHeader.picture_coding_type == 1 ? "I" : (picHeader.picture_coding_type == 2 ? "P" : "B"))
                    + ", temp_ref:" + picHeader.temporal_reference + ">", MainUtils.ANSIColor.BROWN, true));
        }
    }
}
