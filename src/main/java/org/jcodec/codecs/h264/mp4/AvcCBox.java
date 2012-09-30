package org.jcodec.codecs.h264.mp4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.StreamParams;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.wav.StringReader;
import org.jcodec.common.io.ReaderBE;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author Jay Codec
 * 
 */
public class AvcCBox extends Box implements StreamParams {

    private List<SeqParameterSet> spsList = new ArrayList<SeqParameterSet>();
    private List<PictureParameterSet> ppsList = new ArrayList<PictureParameterSet>();

    public AvcCBox(Box other) {
        super(other);
    }

    public AvcCBox() {
        super(new Header(fourcc()));
    }

    public AvcCBox(Header header) {
        super(header);
    }

    public AvcCBox(List<SeqParameterSet> spsList, List<PictureParameterSet> ppsList) {
        this();
        this.spsList = spsList;
        this.ppsList = ppsList;
    }

    public static String fourcc() {
        return "avcC";
    }

    @Override
    public void parse(InputStream input) throws IOException {
        StringReader.sureSkip(input, 5);
        int nSPS = input.read() & 0x1f; // 3 bits reserved + 5 bits number of
                                        // sps
        for (int i = 0; i < nSPS; i++) {
            int spsSize = (int) ReaderBE.readInt16(input);
            byte[] sps = new byte[spsSize];
            input.read(sps);
            spsList.add(SeqParameterSet.read(new ByteArrayInputStream(sps, 1, spsSize - 1)));
        }

        int nPPS = input.read() & 0xff;
        for (int i = 0; i < nPPS; i++) {
            int ppsSize = (int) ReaderBE.readInt16(input);
            byte[] pps = new byte[ppsSize];
            input.read(pps);
            ppsList.add(PictureParameterSet.read(new ByteArrayInputStream(pps, 1, ppsSize - 1)));
        }
    }

    @Override
    protected void doWrite(DataOutput out) throws IOException {
        out.write(0x1);
        out.write(0x64);
        out.write(0x0);
        out.write(0x1e);
        out.write(0xff);

        out.write(spsList.size() | 0xe0);
        for (SeqParameterSet sps : spsList) {
            byte[] b = toByteArray(sps);
            out.writeShort(b.length + 1);
            out.write(0x67);
            out.write(b);
        }

        out.write(ppsList.size());
        for (PictureParameterSet pps : ppsList) {
            byte[] b = toByteArray(pps);
            out.writeShort(b.length + 1);
            out.write(0x68);
            out.write(b);
        }
    }

    private static byte[] toByteArray(SeqParameterSet sps) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sps.write(out);
        return out.toByteArray();
    }

    private static byte[] toByteArray(PictureParameterSet pps) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pps.write(out);
        return out.toByteArray();
    }

    public List<SeqParameterSet> getSpsList() {
        return spsList;
    }

    public List<PictureParameterSet> getPpsList() {
        return ppsList;
    }

    public SeqParameterSet getSPS(int id) {
        for (SeqParameterSet sps : spsList) {
            if (sps.seq_parameter_set_id == id)
                return sps;
        }
        return null;
    }

    public PictureParameterSet getPPS(int id) {
        for (PictureParameterSet pps : ppsList) {
            if (pps.pic_parameter_set_id == id)
                return pps;
        }
        return null;
    }
    
    public AvcCBox copy() {
        List<SeqParameterSet> nSpsList = new ArrayList<SeqParameterSet>();
        for (SeqParameterSet sps : spsList) {
            nSpsList.add(sps.copy());
        }
        List<PictureParameterSet> nPpsList = new ArrayList<PictureParameterSet>();
        for (PictureParameterSet pps : ppsList) {
            nPpsList.add(pps.copy());
        }
        return new AvcCBox(nSpsList, nPpsList);
    }
}