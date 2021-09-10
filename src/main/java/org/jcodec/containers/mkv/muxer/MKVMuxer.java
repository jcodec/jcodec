package org.jcodec.containers.mkv.muxer;

import static org.jcodec.containers.mkv.MKVType.CodecID;
import static org.jcodec.containers.mkv.MKVType.Cues;
import static org.jcodec.containers.mkv.MKVType.DateUTC;
import static org.jcodec.containers.mkv.MKVType.DocType;
import static org.jcodec.containers.mkv.MKVType.DocTypeReadVersion;
import static org.jcodec.containers.mkv.MKVType.DocTypeVersion;
import static org.jcodec.containers.mkv.MKVType.EBML;
import static org.jcodec.containers.mkv.MKVType.EBMLMaxIDLength;
import static org.jcodec.containers.mkv.MKVType.EBMLMaxSizeLength;
import static org.jcodec.containers.mkv.MKVType.EBMLReadVersion;
import static org.jcodec.containers.mkv.MKVType.EBMLVersion;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.MuxingApp;
import static org.jcodec.containers.mkv.MKVType.Name;
import static org.jcodec.containers.mkv.MKVType.PixelHeight;
import static org.jcodec.containers.mkv.MKVType.PixelWidth;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.TrackUID;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.Video;
import static org.jcodec.containers.mkv.MKVType.WritingApp;
import static org.jcodec.containers.mkv.MKVType.createByType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Codec;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mkv.CuesFactory;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.SeekHeadFactory;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlDate;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.muxer.MKVMuxerTrack.MKVMuxerTrackType;
import org.jcodec.containers.mkv.util.EbmlUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MKVMuxer implements Muxer {

    private List<MKVMuxerTrack> tracks;
    private MKVMuxerTrack audioTrack;
    private MKVMuxerTrack videoTrack;
    private EbmlMaster mkvInfo;
    private EbmlMaster mkvTracks;
    private EbmlMaster mkvCues;
    private EbmlMaster mkvSeekHead;
    private SeekableByteChannel sink;
    private CuesFactory cf;
    private long afterebmlHeader, beforeClusters, beforeCues, afterSegment, eof;

    private static Map<Codec, String> codec2mkv = new HashMap<Codec, String>();
    static {
        codec2mkv.put(Codec.H264, "V_MPEG4/ISO/AVC");
        codec2mkv.put(Codec.VP8, "V_VP8");
        codec2mkv.put(Codec.VP9, "V_VP9");
    }

    public MKVMuxer(SeekableByteChannel s) {
        this.sink = s;
        this.tracks = new ArrayList<MKVMuxerTrack>();
        try {
            defaultEbmlHeader().mux(sink);
            afterebmlHeader = sink.position();
            mkvTracks = createByType(Tracks);
            // We leave a gap to make sure we have space for the headers that we can only do
            // once we finalise the file
            sink.setPosition(
                    afterebmlHeader + 12 /* segment basics */ + 68 /* seekhead */ + 50 /* info */ + 106/* tracks */);
            beforeClusters = sink.position();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public MKVMuxerTrack createVideoTrack(VideoCodecMeta meta, String codecId) {
        if (videoTrack == null) {
            cf = new CuesFactory(beforeClusters, tracks.size() + 1);
            videoTrack = new MKVMuxerTrack(sink, cf);
            tracks.add(videoTrack);
            videoTrack.codecId = codecId;
            videoTrack.videoMeta = meta;
            videoTrack.trackNo = tracks.size();
            muxTrack(videoTrack);
        }
        return videoTrack;
    }

    public void finish() throws IOException {
        for (MKVMuxerTrack track : tracks) {
            track.finish();
        }
        beforeCues = sink.position();
        mkvCues = (EbmlMaster) createByType(Cues);
        muxCues();
        mkvCues.mux(sink);
        eof = sink.position();

        sink.setPosition(afterebmlHeader);

        // Segment basic details
        ByteBuffer segmentHeader = ByteBuffer.allocate(12);
        segmentHeader.put(Segment.id);
        segmentHeader.put(EbmlUtil.ebmlEncodeLen(eof - afterebmlHeader - 12, 8));
        segmentHeader.flip();
        sink.write(segmentHeader);
        afterSegment = sink.position();

        mkvInfo = muxInfo();
        mkvSeekHead = muxSeekHead();

        mkvSeekHead.mux(sink);
        mkvInfo.mux(sink);
        mkvTracks.mux(sink);
        long endOfHeaders = sink.position();
        long needsVoid = beforeClusters - endOfHeaders;
        ByteBuffer voidFiller = ByteBuffer.allocate((int) needsVoid);
        voidFiller.put(MKVType.Void.id);
        voidFiller.put(EbmlUtil.ebmlEncodeLen(needsVoid - 1 - 8, 8));
        voidFiller.flip();
        sink.write(voidFiller);
    }

    private EbmlMaster defaultEbmlHeader() {
        EbmlMaster master = (EbmlMaster) createByType(EBML);

        createLong(master, EBMLVersion, 1);
        createLong(master, EBMLReadVersion, 1);
        createLong(master, EBMLMaxIDLength, 4);
        createLong(master, EBMLMaxSizeLength, 8);

        createString(master, DocType, "webm");
        createLong(master, DocTypeVersion, 2);
        createLong(master, DocTypeReadVersion, 2);

        return master;
    }

    private EbmlMaster muxInfo() {
        EbmlMaster master = (EbmlMaster) createByType(Info);
        Rational fr = tracks.get(0).getFrameRate();
        createLong(master, TimecodeScale, (long) (fr.toDouble() * MKVMuxerTrack.DEFAULT_TIMESCALE));
        createString(master, WritingApp, "JCodec");
        createString(master, MuxingApp, "JCodec");

        List<MKVMuxerTrack> tracks2 = tracks;
        long max = 0;
        for (MKVMuxerTrack track : tracks2) {
            MkvBlock lastBlock = track.lastFrame;
            if (lastBlock.absoluteTimecode > max)
                max = lastBlock.absoluteTimecode;
        }
        createDouble(master, MKVType.Duration, (max + 1) * 1.0);
        createDate(master, DateUTC, new Date());
        return master;
    }

    private void muxTrack(MKVMuxerTrack track) {
        try {
            track.trackStart = sink.position();
            EbmlMaster trackEntryElem = (EbmlMaster) createByType(TrackEntry);

            createLong(trackEntryElem, TrackNumber, track.trackNo);

            createLong(trackEntryElem, TrackUID, track.trackNo);
            if (MKVMuxerTrackType.VIDEO.equals(track.type)) {
                createLong(trackEntryElem, TrackType, (byte) 0x01);
                createString(trackEntryElem, Name, "Track " + track.trackNo + " Video");
                createString(trackEntryElem, CodecID, track.codecId);
                // createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
                // VideoCodecMeta vcm = (VideoCodecMeta) codecMeta;

                EbmlMaster trackVideoElem = (EbmlMaster) createByType(Video);
                createLong(trackVideoElem, PixelWidth, track.videoMeta.getSize().getWidth());
                createLong(trackVideoElem, PixelHeight, track.videoMeta.getSize().getHeight());

                trackEntryElem.add(trackVideoElem);

            } else {
                createLong(trackEntryElem, TrackType, (byte) 0x02);
                createString(trackEntryElem, Name, "Track " + track.trackNo + " Audio");
                createString(trackEntryElem, CodecID, track.codecId);
                // createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
            }
            mkvTracks.add(trackEntryElem);
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    private void muxCues() {
        EbmlMaster indexedCues = cf.createCues();

        for (EbmlBase aCuePoint : indexedCues.children)
            mkvCues.add(aCuePoint);
    }

    private EbmlMaster muxSeekHead() {
        SeekHeadFactory shi = new SeekHeadFactory(beforeCues - afterSegment);
        shi.add(mkvInfo);
        shi.add(mkvTracks);
        shi.add(mkvCues);
        return shi.indexSeekHead();
    }

    public static void createLong(EbmlMaster parent, MKVType type, long value) {
        EbmlUint se = (EbmlUint) createByType(type);
        se.setUint(value);
        parent.add(se);
    }

    public static void createString(EbmlMaster parent, MKVType type, String value) {
        EbmlString se = (EbmlString) createByType(type);
        se.setString(value);
        parent.add(se);
    }

    public static void createDate(EbmlMaster parent, MKVType type, Date value) {
        EbmlDate se = (EbmlDate) createByType(type);
        se.setDate(value);
        parent.add(se);
    }

    public static void createBuffer(EbmlMaster parent, MKVType type, ByteBuffer value) {
        EbmlBin se = (EbmlBin) createByType(type);
        se.setBuf(value);
        parent.add(se);
    }

    public static void createDouble(EbmlMaster parent, MKVType type, double value) {
        try {
            EbmlFloat se = (EbmlFloat) createByType(type);
            se.setDouble(value);
            parent.add(se);
        } catch (ClassCastException cce) {
            throw new RuntimeException("Element of type " + type + " can't be cast to EbmlFloat", cce);
        }
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        return createVideoTrack(meta, codec2mkv.get(codec));
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        audioTrack = new MKVMuxerTrack(sink, cf);
        audioTrack.type = MKVMuxerTrackType.AUDIO;
        tracks.add(audioTrack);
        audioTrack.codecId = codec2mkv.get(codec);
        audioTrack.trackNo = tracks.size();
        muxTrack(audioTrack);
        return audioTrack;
    }
}