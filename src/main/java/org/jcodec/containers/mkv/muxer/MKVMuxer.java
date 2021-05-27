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
import java.util.LinkedList;
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
    private List<EbmlMaster> clusterList;
    private SeekableByteChannel sink;
    
    private static Map<Codec, String> codec2mkv = new HashMap<Codec, String>();
    static {
        codec2mkv.put(Codec.H264, "V_MPEG4/ISO/AVC");
        codec2mkv.put(Codec.VP8, "V_VP8");
        codec2mkv.put(Codec.VP9, "V_VP9");
    }

    public MKVMuxer(SeekableByteChannel s) {
        this.sink = s;
        this.tracks = new ArrayList<MKVMuxerTrack>();
        this.clusterList = new LinkedList<EbmlMaster>();
    }

    public MKVMuxerTrack createVideoTrack(VideoCodecMeta meta, String codecId) {
        if (videoTrack == null) {
            videoTrack = new MKVMuxerTrack();
            tracks.add(videoTrack);
            videoTrack.codecId = codecId;
            videoTrack.videoMeta = meta;
            videoTrack.trackNo = tracks.size();
        }
        return videoTrack;
    }

    public void finish() throws IOException {
        List<EbmlMaster> mkvFile = new ArrayList<EbmlMaster>();
        EbmlMaster ebmlHeader = defaultEbmlHeader();
        mkvFile.add(ebmlHeader);

        EbmlMaster segmentElem = (EbmlMaster) createByType(Segment);
        mkvInfo = muxInfo();
        mkvTracks = muxTracks();
        mkvCues = (EbmlMaster) createByType(Cues);
        mkvSeekHead = muxSeekHead();
        createClusters();
        muxCues();

        segmentElem.add(mkvSeekHead);
        segmentElem.add(mkvInfo);
        segmentElem.add(mkvTracks);
        segmentElem.add(mkvCues);
        for (EbmlMaster aCluster : clusterList)
            segmentElem.add(aCluster);
        mkvFile.add(segmentElem);

        for (EbmlMaster el : mkvFile)
            el.mux(sink);
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
        Rational fr=tracks.get(0).getFrameRate();
        createLong(master, TimecodeScale, (long)(fr.toDouble()*MKVMuxerTrack.DEFAULT_TIMESCALE));
        createString(master, WritingApp, "JCodec");
        createString(master, MuxingApp, "JCodec");

        List<MKVMuxerTrack> tracks2 = tracks;
        long max = 0;
        for (MKVMuxerTrack track : tracks2) {
            MkvBlock lastBlock = track.trackBlocks.get(track.trackBlocks.size() - 1);
            if (lastBlock.absoluteTimecode > max)
                max = lastBlock.absoluteTimecode;
        }
        createDouble(master, MKVType.Duration, (max + 1) * 1.0);
        createDate(master, DateUTC, new Date());
        return master;
    }

    private EbmlMaster muxTracks() {
        EbmlMaster master = (EbmlMaster) createByType(Tracks);
        for (int i = 0; i < tracks.size(); i++) {
            MKVMuxerTrack track = tracks.get(i);
            EbmlMaster trackEntryElem = (EbmlMaster) createByType(TrackEntry);

            createLong(trackEntryElem, TrackNumber, track.trackNo);

            createLong(trackEntryElem, TrackUID, track.trackNo);
            if (MKVMuxerTrackType.VIDEO.equals(track.type)) {
                createLong(trackEntryElem, TrackType, (byte) 0x01);
                createString(trackEntryElem, Name, "Track " + (i + 1) + " Video");
                createString(trackEntryElem, CodecID, track.codecId);
                //                createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
                //                VideoCodecMeta vcm = (VideoCodecMeta) codecMeta;

                EbmlMaster trackVideoElem = (EbmlMaster) createByType(Video);
                createLong(trackVideoElem, PixelWidth, track.videoMeta.getSize().getWidth());
                createLong(trackVideoElem, PixelHeight, track.videoMeta.getSize().getHeight());

                trackEntryElem.add(trackVideoElem);

            } else {
                createLong(trackEntryElem, TrackType, (byte) 0x02);
                createString(trackEntryElem, Name, "Track " + (i + 1) + " Audio");
                createString(trackEntryElem, CodecID, track.codecId);
                //                createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
            }

            master.add(trackEntryElem);
        }
        return master;
    }

    private void createClusters() { // Creates one cluster per each keyframe
    	EbmlMaster mkvCluster=null;
        for (MkvBlock aBlock : videoTrack.trackBlocks) {
        	if(aBlock._keyFrame) {
        		mkvCluster=singleBlockedCluster(aBlock);
        		clusterList.add(mkvCluster);
        	} else if(mkvCluster==null) {
        		throw new RuntimeException("The first frame must be a keyframe in an MKV file");
        	} else {
        		mkvCluster.add(aBlock); // intraframes don't get their own cluster
        	}
        }
    	
    }
    
    private void muxCues() {
        CuesFactory cf = new CuesFactory(mkvSeekHead.size() + mkvInfo.size() + mkvTracks.size(),
                videoTrack.trackNo);
        for(EbmlMaster mkvCluster:clusterList) {
            cf.add(CuesFactory.CuePointMock.make(mkvCluster));
        }

        EbmlMaster indexedCues = cf.createCues();

        for (EbmlBase aCuePoint : indexedCues.children)
            mkvCues.add(aCuePoint);
    }

    private EbmlMaster singleBlockedCluster(MkvBlock aBlock) {
        EbmlMaster mkvCluster = createByType(MKVType.Cluster);
        createLong(mkvCluster, MKVType.Timecode, aBlock.absoluteTimecode - aBlock.timecode);
        mkvCluster.add(aBlock);
        return mkvCluster;
    }

    private EbmlMaster muxSeekHead() {
        SeekHeadFactory shi = new SeekHeadFactory();
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
        audioTrack = new MKVMuxerTrack();
        tracks.add(audioTrack);
        audioTrack.codecId = codec2mkv.get(codec);
        audioTrack.trackNo = tracks.size();
        return audioTrack;
    }
}