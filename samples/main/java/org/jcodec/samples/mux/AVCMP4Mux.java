package org.jcodec.samples.mux;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jcodec.codecs.h264.annexb.NALUnitReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.FileRAOutputStream;
import org.jcodec.common.io.RAOutputStream;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.SampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class AVCMP4Mux {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: <in.264> <out.mp4>\n" + "\tWhere:\n"
                    + "\t-q\tLook for stream parameters only in the beginning of stream");
            return;
        }

        File in = new File(args[0]);
        File out = new File(args[1]);

        List<SeqParameterSet> spsList = new ArrayList<SeqParameterSet>();
        List<PictureParameterSet> ppsList = new ArrayList<PictureParameterSet>();

        RAOutputStream file = new FileRAOutputStream(out);
        MP4Muxer muxer = new MP4Muxer(file);
        CompressedTrack track = muxer.addTrackForCompressed(TrackType.VIDEO, 25);

        mux(track, in, spsList, ppsList);

        Size size = new Size((spsList.get(0).pic_width_in_mbs_minus1 + 1) << 4,
                (spsList.get(0).pic_height_in_map_units_minus1 + 1) << 4);

        SampleEntry se = MP4Muxer.videoSampleEntry("avc1", size, "JCodec");

        se.add(new AvcCBox(spsList, ppsList));
        track.addSampleEntry(se);

        muxer.writeHeader();

        file.close();
    }

    private static void mux(CompressedTrack track, File f, List<SeqParameterSet> spsList,
            List<PictureParameterSet> ppsList) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            NALUnitReader in = new NALUnitReader(is);
            InputStream nextNALUnit = null;
            int i = 0;
            do {
                nextNALUnit = in.nextNALUnit();
                if (nextNALUnit == null)
                    continue;
                NALUnit nu = NALUnit.read(nextNALUnit);
                if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {

                    track.addFrame(new MP4Packet(formPacket(nu, nextNALUnit), i, 25, 1, i,
                            nu.type == NALUnitType.IDR_SLICE, null, i, 0));
                    i++;
                } else if (nu.type == NALUnitType.SPS) {
                    spsList.add(SeqParameterSet.read(nextNALUnit));
                } else if (nu.type == NALUnitType.PPS) {
                    ppsList.add(PictureParameterSet.read(nextNALUnit));
                } else {
                    nextNALUnit.skip(Integer.MAX_VALUE);
                }
            } while (nextNALUnit != null);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static Buffer formPacket(NALUnit nu, InputStream nextNALUnit) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        byte[] data = IOUtils.toByteArray(nextNALUnit);
        out.writeInt(data.length + 1);
        nu.write(out);
        out.write(data);
        return new Buffer(baos.toByteArray());
    }
}
