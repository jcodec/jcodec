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
            boolean dumpMarkers = false;
            Long dumpAfterPts = cmd.getLongFlag(DUMP_FROM);
            Long stopPts = cmd.getLongFlag(STOP_AT);

            PESPacket pkt = null;
            while (ch.position() + 4 < ch.size()) {
                long bufferOffset = ch.position();
                ByteBuffer buffer = NIOUtils.fetchFrom(ch, 0x100000);

                while (buffer.remaining() >= 8) {
                    while (buffer.remaining() >= 8) {
                        int marker = buffer.duplicate().getInt();
                        if (marker >= 0x1bd && marker <= 0x1ff && marker != 0x1be)
                            break;
                        int mark = buffer.getInt();
                        ByteBuffer b = MPEGUtil.gotoNextMarker(buffer);
                        if (dumpMarkers && pkt != null && pkt.streamId >= 0xe0 && mark <= 0x1b8 & mark >= 0x100)
                            dumpMarker(mark, b);

                    }
                    if (buffer.remaining() < 4)
                        break;

                    pkt = readPESHeader(buffer, buffer.position() + bufferOffset);
                    System.out.println(pkt.streamId + "(" + (pkt.streamId >= 0xe0 ? "video" : "audio") + ")" + " ["
                            + pkt.pos + "], pts: " + pkt.pts + ", dts: " + pkt.dts);
                    if (dumpAfterPts != null && pkt.pts >= dumpAfterPts)
                        dumpMarkers = true;
                    if (stopPts != null && pkt.pts >= stopPts)
                        return;
                }
                ch.position(ch.position() + buffer.position());
            }
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    private static void dumpMarker(int mark, ByteBuffer b) {
        System.out.print(String.format("marker: 0x%02x ( ", mark));
        if (mark == 0x100)
            dumpPictureHeader(b);
        else if (mark <= 0x1af)
            System.out.print(MainUtils.color(String.format("slice @0x%02x", mark - 0x101), MainUtils.ANSIColor.BLACK,
                    true));
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
                + (gopHeader.getTimeCode() != null ? (",timecode:" + gopHeader.getTimeCode().toString()) : "") + ">",
                MainUtils.ANSIColor.MAGENTA, true));
    }

    private static void dumpSequenceHeader() {
        System.out.print(MainUtils.color("sequence header", MainUtils.ANSIColor.BLUE, true));
    }

    private static void dumpPictureHeader(ByteBuffer b) {
        PictureHeader picHeader = PictureHeader.read(b);
        System.out.print(MainUtils.color("picture header" + " <type:"
                + (picHeader.picture_coding_type == 0 ? "I" : (picHeader.picture_coding_type == 1 ? "P" : "B")) + ">",
                MainUtils.ANSIColor.BROWN, true));
    }
}
