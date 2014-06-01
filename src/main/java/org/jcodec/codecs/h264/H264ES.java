package org.jcodec.codecs.h264;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts H.264 frames out H.264 Elementary stream ( according to Annex B )
 * 
 * @author Jay Codec
 * 
 */
public class H264ES extends BaseH264ES {

    private ReadableByteChannel ch;
    private ByteBuffer buf;
    private int marker;
    private ByteBuffer nalBuf;

    public H264ES(ReadableByteChannel ch) throws IOException {
        this.ch = ch;
        this.frameNo = 0;
        this.buf = ByteBuffer.allocate(4096);
        ch.read(buf);
        buf.flip();
        marker = buf.getInt();
    }

    private class NUSH {
        public NALUnit nu;
        public SliceHeader sh;

        public NUSH(NALUnit nu, SliceHeader sh) {
            this.nu = nu;
            this.sh = sh;
        }
    }

    public Packet nextFrame(ByteBuffer out) throws IOException {
        ByteBuffer dup = out.duplicate();

        NUSH prev = null;

        if (nalBuf != null) {
            prev = processNAL(nalBuf);
            dup.put(nalBuf);
            nalBuf.clear();
        } else
            nalBuf = ByteBuffer.allocate(1024 * 1024);

        while (true) {
            if (!buf.hasRemaining()) {
                buf.clear();
                ch.read(buf);
                buf.flip();

                if (!buf.hasRemaining()) {
                    nalBuf.flip();
                    break;
                }
            }

            nalBuf.put((byte) (marker >>> 24));
            marker <<= 8;
            marker |= (buf.get() & 0xff);

            if ((marker & 0xffffff) == 1) {
                nalBuf.flip();
                NUSH thiis = processNAL(nalBuf);

                if (thiis != null) {
                    if (prev != null && !sameFrame(prev.nu, thiis.nu, prev.sh, thiis.sh))
                        break;
                    prev = thiis;
                }

                dup.put(nalBuf);
                nalBuf.clear();
            }
        }
        dup.flip();

        return dup.remaining() == 0 ? null : detectPoc(dup, prev.nu, prev.sh);
    }

    private NUSH processNAL(ByteBuffer buf) {
        ByteBuffer dd = buf.duplicate();

        H264Utils.skipToNALUnit(dd);
        if (!dd.hasRemaining())
            return null;
        NALUnit nu = NALUnit.read(dd);

        SliceHeader sh = null;
        if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
            sh = readSliceHeader(dd, nu);
            return new NUSH(nu, sh);
        } else if (nu.type == NALUnitType.PPS) {
            PictureParameterSet read = H264Utils.readPPS(dd);
            pps.put(read.pic_parameter_set_id, read);
        } else if (nu.type == NALUnitType.SPS) {
            SeqParameterSet read = H264Utils.readSPS(dd);
            sps.put(read.seq_parameter_set_id, read);
        }
        return null;
    }
}