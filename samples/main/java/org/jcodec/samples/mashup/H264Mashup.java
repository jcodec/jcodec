package org.jcodec.samples.mashup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.write.NALUnitWriter;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class H264Mashup {
    private SeqParameterSet sps;
    private PictureParameterSet pps;
    private int lastSPS;
    private int lastPPS;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Syntax: <in 1> <in 2> <out>");
            System.exit(-1);
        }
        new H264Mashup().mashup(new File(args[0]), new File(args[1]), new File(args[2]));
    }

    public void mashup(File if1, File if2, File of) throws IOException {
        {
            MappedByteBuffer map = NIOUtils.map(if1);
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(of);
                NALUnitWriter out = new NALUnitWriter(os.getChannel());
                justCopy(out, map);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }

        {
            FileOutputStream os = null;
            try {
                MappedByteBuffer map = NIOUtils.map(if1);
                os = new FileOutputStream(of, true);

                NALUnitWriter out = new NALUnitWriter(os.getChannel());

                copyModify(out, map);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }

    }

    private void justCopy(NALUnitWriter out, ByteBuffer buf) throws IOException {
        ByteBuffer nus;
        while ((nus = H264Utils.nextNALUnit(buf)) != null) {
            NALUnit nu = NALUnit.read(nus);
            if (nu.type == NALUnitType.SPS) {
                out.writeUnit(nu, nus.duplicate());
                sps = SeqParameterSet.read(nus);
                if (sps.seq_parameter_set_id > lastSPS)
                    lastSPS = sps.seq_parameter_set_id;
            } else if (nu.type == NALUnitType.PPS) {
                out.writeUnit(nu, nus.duplicate());
                pps = PictureParameterSet.read(nus);
                if (pps.pic_parameter_set_id > lastPPS)
                    lastPPS = pps.pic_parameter_set_id;
            } else {
                out.writeUnit(nu, nus);
            }
        }
    }

    private void copyModify(NALUnitWriter out, ByteBuffer buf) throws IOException {
        SliceHeaderReader reader = null;
        SliceHeaderWriter writer = null;
        ByteBuffer nus;
        while ((nus = H264Utils.nextNALUnit(buf)) != null) {
            NALUnit nu = NALUnit.read(nus);
            if (nu.type == NALUnitType.SPS) {
                out.writeUnit(nu, nus.duplicate());
                sps = SeqParameterSet.read(nus);
                sps.seq_parameter_set_id = ++lastSPS;
                System.out.println("SPS");
            } else if (nu.type == NALUnitType.PPS) {
                out.writeUnit(nu, nus.duplicate());
                pps = PictureParameterSet.read(nus);
                pps.seq_parameter_set_id = lastSPS;
                pps.pic_parameter_set_id = ++lastPPS;
                reader = new SliceHeaderReader();
                writer = new SliceHeaderWriter();
                System.out.println("PPS");
            } else if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
                ByteBuffer res = ByteBuffer.allocate(nus.remaining() + 10);
                BitReader r = new BitReader(nus);
                SliceHeader header = reader.readPart1(r);
                reader.readPart2(header, nu, sps, pps, r);
                header.pic_parameter_set_id = lastPPS;
                BitWriter w = new BitWriter(res);
                writer.write(header, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, w);

                if (pps.entropy_coding_mode_flag) {
                    copyCABAC(w, r);
                } else {
                    copyCAVLC(w, r);
                }
                res.flip();
                out.writeUnit(nu, res);
            } else {
                out.writeUnit(nu, nus);
            }
        }
    }

    private void copyCAVLC(BitWriter w, BitReader r) {
        int rem = 8 - r.curBit();
        int l = r.readNBit(rem);
        w.writeNBit(l, rem);
        int b = r.readNBit(8), next;
        while ((next = r.readNBit(8)) != -1) {
            w.writeNBit(b, 8);
            b = next;
        }
        int len = 7;
        while ((b & 0x1) == 0) {
            b >>= 1;
            len--;
        }
        w.writeNBit(b, len);
        w.write1Bit(1);
        w.flush();
    }

    private void copyCABAC(BitWriter w, BitReader r) {
        long bp = r.curBit();
        long rem = r.readNBit(8 - (int) bp);
        Assert.assertEquals(rem, (1 << (8 - bp)) - 1);

        if (w.curBit() != 0)
            w.writeNBit(0xff, 8 - w.curBit());
        int b;
        while ((b = r.readNBit(8)) != -1)
            w.writeNBit(b, 8);
    }
}
