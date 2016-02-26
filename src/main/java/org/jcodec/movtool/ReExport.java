package org.jcodec.movtool;

import java.io.File;
import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresFix;
import org.jcodec.containers.mp4.MP4Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Re-exports a prores movie
 * 
 * @author The JCodec project
 * 
 */
public class ReExport extends Remux {

    private ByteBuffer outBuf;

    protected MP4Packet processFrame(MP4Packet pkt) {
        if (outBuf == null) {
            outBuf = ByteBuffer.allocate(pkt.getData().remaining() * 2);
        }
        ByteBuffer out = ProresFix.transcode(pkt.getData(), outBuf);

        return new MP4Packet(pkt, out);
    }

    public static void main2(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("reexport <movie> <out>");
            return;
        }

        File tgt = new File(args[0]);
        File src = hidFile(tgt);
        tgt.renameTo(src);

        try {
            new ReExport().remux(tgt, src, null, null);
        } catch (Throwable t) {
            t.printStackTrace();
            tgt.renameTo(new File(tgt.getParentFile(), tgt.getName() + ".error"));
            src.renameTo(tgt);
        }
    }
}