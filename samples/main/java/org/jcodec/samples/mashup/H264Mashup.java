package org.jcodec.samples.mashup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.StreamParams;
import org.jcodec.codecs.h264.annexb.NALUnitReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.read.SliceHeaderReader;
import org.jcodec.codecs.h264.io.write.NALUnitWriter;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.codecs.h264.io.write.WritableTransportUnit;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
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
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(new FileInputStream(if1));
                os = new BufferedOutputStream(new FileOutputStream(of));

                NALUnitWriter out = new NALUnitWriter(os);
                NALUnitReader in = new NALUnitReader(is);
                justCopy(out, in);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
        }

        {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(new FileInputStream(if2));
                os = new BufferedOutputStream(new FileOutputStream(of, true));

                NALUnitWriter out = new NALUnitWriter(os);
                NALUnitReader in = new NALUnitReader(is);

                copyModify(out, in);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
        }

    }

    private void justCopy(NALUnitWriter out, NALUnitReader in) throws IOException {
        InputStream nus;
        while ((nus = in.nextNALUnit()) != null) {
            NALUnit nu = NALUnit.read(nus);
            if (nu.type == NALUnitType.SPS) {
                WritableTransportUnit ounit = out.writeUnit(nu);
                sps = SeqParameterSet.read(nus);
                sps.write(ounit.getOutputStream());
                if (sps.seq_parameter_set_id > lastSPS)
                    lastSPS = sps.seq_parameter_set_id;
            } else if (nu.type == NALUnitType.PPS) {
                WritableTransportUnit ounit = out.writeUnit(nu);
                pps = PictureParameterSet.read(nus);
                pps.write(ounit.getOutputStream());
                if (pps.pic_parameter_set_id > lastPPS)
                    lastPPS = pps.pic_parameter_set_id;
            } else {
                WritableTransportUnit ounit = out.writeUnit(nu);
                IOUtils.copy(nus, ounit.getOutputStream());
            }
        }
    }

    private void copyModify(NALUnitWriter out, NALUnitReader in) throws IOException {
        SliceHeaderReader reader = null;
        SliceHeaderWriter writer = null;
        InputStream nus;
        while ((nus = in.nextNALUnit()) != null) {
            NALUnit nu = NALUnit.read(nus);
            if (nu.type == NALUnitType.SPS) {
                WritableTransportUnit ounit = out.writeUnit(nu);
                sps = SeqParameterSet.read(nus);
                sps.seq_parameter_set_id = ++lastSPS;
                sps.write(ounit.getOutputStream());
                System.out.println("SPS");
            } else if (nu.type == NALUnitType.PPS) {
                WritableTransportUnit ounit = out.writeUnit(nu);
                pps = PictureParameterSet.read(nus);
                pps.seq_parameter_set_id = lastSPS;
                pps.pic_parameter_set_id = ++lastPPS;
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
                header.pic_parameter_set_id = lastPPS;
                writer.write(header, nu.type == NALUnitType.IDR_SLICE, nu.nal_ref_idc, w);

                if (pps.entropy_coding_mode_flag) {
                    copyCABAC(w, r);
                } else {
                    copyCAVLC(w, r);
                }
            } else {
                WritableTransportUnit ounit = out.writeUnit(nu);
                IOUtils.copy(nus, ounit.getOutputStream());
            }
        }
    }

    private void copyCAVLC(OutBits w, InBits r) throws IOException {
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

    private void copyCABAC(OutBits w, InBits r) throws IOException {
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
