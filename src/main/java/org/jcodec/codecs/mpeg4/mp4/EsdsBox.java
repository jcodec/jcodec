package org.jcodec.codecs.mpeg4.mp4;

import java.nio.ByteBuffer;

import org.jcodec.codecs.mpeg4.es.DecoderConfig;
import org.jcodec.codecs.mpeg4.es.DecoderSpecific;
import org.jcodec.codecs.mpeg4.es.Descriptor;
import org.jcodec.codecs.mpeg4.es.ES;
import org.jcodec.codecs.mpeg4.es.SL;
import org.jcodec.common.io.BitWriter;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.FullBox;
import org.jcodec.containers.mp4.boxes.Header;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 4 elementary stream descriptor
 * 
 * @author The JCodec project
 * 
 */
public class EsdsBox extends FullBox {

    private ByteBuffer streamInfo;
    private int objectType;
    private int bufSize;
    private int maxBitrate;
    private int avgBitrate;
    private int trackId;

    public static String fourcc() {
        return "esds";
    }

    public EsdsBox(Header atom) {
        super(atom);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        if (streamInfo != null && streamInfo.remaining() > 0)
            new ES(trackId,
                    new Descriptor[]{new DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, new Descriptor[]{ new DecoderSpecific(streamInfo)}),
                    new SL()}).write(out);
        else
            new ES(trackId, new Descriptor[]{new DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, new Descriptor[0]), new SL()}).write(out);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        ES es = (ES) Descriptor.read(input);

        trackId = es.getTrackId();
        DecoderConfig decoderConfig = Descriptor.find(es, DecoderConfig.class, DecoderConfig.tag());
        objectType = decoderConfig.getObjectType();
        bufSize = decoderConfig.getBufSize();
        maxBitrate = decoderConfig.getMaxBitrate();
        avgBitrate = decoderConfig.getAvgBitrate();
        DecoderSpecific decoderSpecific = Descriptor.find(decoderConfig, DecoderSpecific.class, DecoderSpecific.tag());
        streamInfo = decoderSpecific.getData();
    }

    public ByteBuffer getStreamInfo() {
        return streamInfo;
    }

    public int getObjectType() {
        return objectType;
    }

    public int getBufSize() {
        return bufSize;
    }

    public int getMaxBitrate() {
        return maxBitrate;
    }

    public int getAvgBitrate() {
        return avgBitrate;
    }

    public int getTrackId() {
        return trackId;
    }

    public static Box fromADTS(org.jcodec.codecs.aac.ADTSParser.Header hdr) {
        ByteBuffer si = ByteBuffer.allocate(2);
        BitWriter wr = new BitWriter(si);
        wr.writeNBit(hdr.getObjectType(), 5);
        wr.writeNBit(hdr.getSamplingIndex(), 4);
        wr.writeNBit(hdr.getChanConfig(), 4);
        wr.flush();
        si.clear();

        return createEsdsBox(si, hdr.getObjectType() << 5, 0, 210750, 133350, 2);
    }

    public static EsdsBox createEsdsBox(ByteBuffer streamInfo, int objectType, int bufSize, int maxBitrate,
            int avgBitrate, int trackId) {
        EsdsBox esds = new EsdsBox(new Header(fourcc()));
        esds.objectType = objectType;
        esds.bufSize = bufSize;
        esds.maxBitrate = maxBitrate;
        esds.avgBitrate = avgBitrate;
        esds.trackId = trackId;
        esds.streamInfo = streamInfo;
        return esds;
    }

    public static EsdsBox newEsdsBox() {
        return new EsdsBox(new Header(fourcc()));
    }
}