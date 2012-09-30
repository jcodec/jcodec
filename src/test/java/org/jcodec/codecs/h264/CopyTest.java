package org.jcodec.codecs.h264;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.annexb.NALUnitReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.read.SliceHeaderReader;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.codecs.h264.io.write.NALUnitWriter;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.io.write.WritableTransportUnit;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

public class CopyTest {
    private static SeqParameterSet sps;
    private static PictureParameterSet pps;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: <in> <out>");
            System.exit(-1);
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(args[0]));
            os = new BufferedOutputStream(new FileOutputStream(args[1]));

            NALUnitWriter out = new NALUnitWriter(os);
            NALUnitReader in1 = new NALUnitReader(is);

            SliceHeaderReader reader = null;
            SliceHeaderWriter writer = null;
            InputStream nus;
            while ((nus = in1.nextNALUnit()) != null) {
                NALUnit nu = NALUnit.read(nus);
                if (nu.type == NALUnitType.SPS) {
                    WritableTransportUnit ounit = out.writeUnit(nu);
                    sps = SeqParameterSet.read(nus);
                    sps.seq_parameter_set_id = 1;
                    sps.write(ounit.getOutputStream());
                    System.out.println("SPS");
                } else if (nu.type == NALUnitType.PPS) {
                    WritableTransportUnit ounit = out.writeUnit(nu);
                    pps = PictureParameterSet.read(nus);
                    pps.seq_parameter_set_id = 1;
                    pps.pic_parameter_set_id = 1;
                    pps.write(ounit.getOutputStream());
                    reader = new SliceHeaderReader(new StreamParams() {
                        public SeqParameterSet getSPS(int id) {
                            return sps;
                        }

                        public PictureParameterSet getPPS(int id) {
                            return pps;
                        }
                    });
                    writer = new SliceHeaderWriter(sps, pps);
                    System.out.println("PPS");
                } else if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
                    WritableTransportUnit ounit = out.writeUnit(nu);
                    OutBits w = new BitstreamWriter(ounit.getOutputStream());
                    InBits r = new BitstreamReader(nus);
                    SliceHeader header = reader.read(nu, r);
                    header.pic_parameter_set_id = 1;
                    writer.write(header, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, w);

                    if (pps.entropy_coding_mode_flag) {
                        copyCABAC(w, r);
                    } else {
                        copyCAVLC(w, r);
                    }
                } else {
                    WritableTransportUnit ounit = out.writeUnit(nu);
                    IOUtils.copy(nus, ounit.getOutputStream());
                    System.out.println("OTHER");
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    private static void copyCAVLC(OutBits w, InBits r) throws IOException {
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

    private static void copyCABAC(OutBits w, InBits r) throws IOException {
        long bp = r.curBit();
        long rem = r.readNBit(8 - (int) bp);
        Assert.assertEquals(rem, (1 << (8 - bp)) - 1);

        if (w.curBit() != 0)
            w.writeNBit(0xff, 8 - w.curBit()); // 1 filler
        int b;
        while ((b = r.readNBit(8)) != -1)
            w.writeNBit(b, 8);
    }

    private static boolean bum(byte[] orig, byte[] cool) {
        for (int i = 0; i < cool.length; i++) {
            if (cool[i] != orig[i])
                return false;
        }
        return true;
    }
}
