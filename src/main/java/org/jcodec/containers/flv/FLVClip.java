package org.jcodec.containers.flv;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.jcodec.common.Codec;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.flv.FLVPacket.AvcVideoTagHeader;
import org.jcodec.containers.flv.FLVPacket.Type;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Makes a clip out of an FLV
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVClip {
    private static ByteBuffer avcC;

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.args.length < 2) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("from", "From timestamp (in seconds, i.e 67.49)");
                    put("to", "To timestamp");

                }
            }, "file in", "file out");
            return;
        }

        Double from = cmd.getDoubleFlag("from");
        Double to = cmd.getDoubleFlag("to");
        try (SeekableByteChannel in = NIOUtils.readableFileChannel(new File(cmd.args[0]));
                SeekableByteChannel out = NIOUtils.writableFileChannel(new File(cmd.args[1]))) {
            FLVDemuxer demuxer = new FLVDemuxer(in);
            FLVMuxer muxer = new FLVMuxer(out);
            FLVPacket pkt = null;
            boolean copying = false;
            while ((pkt = demuxer.getPacket()) != null) {
                if (pkt.getType() == Type.VIDEO && pkt.getTagHeader().getCodec() == Codec.H264) {
                    if (((AvcVideoTagHeader) pkt.getTagHeader()).getAvcPacketType() == 0) {
                        avcC = NIOUtils.clone(pkt.getData());
                        System.out.println("GOT AVCC");
                    }
                }

                if (!copying && (from == null || pkt.getPtsD() > from) && pkt.getType() == Type.VIDEO
                        && pkt.isKeyFrame() && avcC != null) {
                    System.out.println("Starting at packet: " + ToJSON.toJSON(pkt));
                    copying = true;
                    muxer.addPacket(new FLVPacket(pkt.getType(), avcC, pkt.getPts(), 0, pkt.getFrameNo(), true, pkt
                            .getMetadata(), 0, pkt.getTagHeader()));
                }

                if (copying)
                    muxer.addPacket(pkt);

                if ((to != null && pkt.getPtsD() >= to)) {
                    System.out.println("Stopping at packet: " + ToJSON.toJSON(pkt));
                    break;
                }
            }
            muxer.finish();
        }
    }
}
