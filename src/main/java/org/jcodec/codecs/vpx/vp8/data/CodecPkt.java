package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.enums.GeneralFrameFlags;
import org.jcodec.codecs.vpx.vp8.enums.PacketKind;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class CodecPkt {
    interface APacket {

    }

    public APacket packet;
    public PacketKind kind;

    public static class FramePacket implements APacket {
        public FullAccessIntArrPointer buf;
        /** < compressed data buffer */
        public int sz;
        /** < length of compressed data */
        /* !\brief time stamp to show frame (in timebase units) */
        public long pts; // int64
        /* !\brief duration to show frame (in timebase units) */
        public long duration; // ulong
        public EnumSet<FrameTypeFlags> vp8flags;
        public EnumSet<GeneralFrameFlags> flags;
        /** < flags for this frame */
        /*
         * !\brief the partition id defines the decoding order of the partitions. Only
         * applicable when "output partition" mode is enabled. First partition has id 0.
         */
        public int partition_id;
        /*
         * !\brief Width and height of the frame in this packet
         */
        public int width;
        /** < frame width */
        public int height;
        /** < frame height */
    }

    public static class PSNRPacket implements APacket {
        public int[] samples = new int[4];
        /** < Number of samples, total/y/u/v */
        public long[] sse = new long[4];
        /** < sum squared error, total/y/u/v */
        public double[] psnr = new double[4]; /** < PSNR, total/y/u/v */

    }
}
