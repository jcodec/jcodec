package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.MKVType.Audio;
import static org.jcodec.containers.mkv.MKVType.BitDepth;
import static org.jcodec.containers.mkv.MKVType.Channels;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.CodecID;
import static org.jcodec.containers.mkv.MKVType.CodecPrivate;
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
import static org.jcodec.containers.mkv.MKVType.SamplingFrequency;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.TrackUID;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.Video;
import static org.jcodec.containers.mkv.MKVType.WritingApp;
import static org.jcodec.containers.mkv.MKVType.createByType;
import static org.jcodec.containers.mkv.muxer.MKVMuxer.createChild;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.Assert;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.boxes.MkvSegment;
import org.jcodec.movtool.streaming.AudioCodecMeta;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.MovieSegment;
import org.jcodec.movtool.streaming.VideoCodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * WebM specific muxing
 * 
 * @author The JCodec project
 * 
 */
public class MKVStreamingMuxer {
    
    private static final int DEFAULT_TIMESCALE = 1000000000; //NANOSECOND
    private static final int TIMESCALE = 1000000;
    private static final int MULTIPLIER = DEFAULT_TIMESCALE/TIMESCALE;

    private static final String VP80_FOURCC = "avc1"; // should be VP80
    private EbmlMaster mkvInfo;
    private EbmlMaster mkvTracks;
    private EbmlMaster mkvCues;
    private EbmlMaster mkvSeekHead;
    private EbmlMaster segmentElem;
    public MovieSegment headerChunk;
    private LinkedList<WebmCluster> webmClusters;
    
    
    public MovieSegment preparePacket(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo, long previousClustersSize) {
        WebmCluster wmc = new WebmCluster(track, pkt, chunkNo, trackNo, previousClustersSize);
        if (webmClusters == null)
            webmClusters = new LinkedList<WebmCluster>();
        webmClusters.add(wmc);
        return wmc;
    }

    public MovieSegment prepareHeader(List<MovieSegment> chunks, VirtualTrack[] tracks) throws IOException {
        EbmlMaster ebmlHeader = muxEbmlHeader();
        segmentElem = (EbmlMaster) createByType(Segment);
        mkvInfo = muxInfo(tracks);
        mkvTracks = muxTracks(tracks);
        mkvCues = (EbmlMaster) createByType(Cues);
        mkvSeekHead = muxSeekHead();
        muxCues(tracks);
        
        // Tracks Info
        segmentElem.add(mkvSeekHead);
        segmentElem.add(mkvInfo);
        segmentElem.add(mkvTracks);
        segmentElem.add(mkvCues);
        
        for (WebmCluster wc : webmClusters)
            segmentElem.add(wc.c);
        
        List<EbmlMaster> header = new ArrayList<EbmlMaster>();
        header.add(ebmlHeader);
        header.add(segmentElem);
        headerChunk  = new HeaderSegment(header);
        
        return headerChunk;
    }
    
    private EbmlMaster muxEbmlHeader(){
        EbmlMaster master = (EbmlMaster) createByType(EBML);

        createChild(master, EBMLVersion, 1);
        createChild(master, EBMLReadVersion, 1);
        createChild(master, EBMLMaxIDLength, 4);
        createChild(master, EBMLMaxSizeLength, 8);
        
        createChild(master, DocType, "webm");
        createChild(master, DocTypeVersion, 2);
        createChild(master, DocTypeReadVersion, 2);

        return master;
    }
    
    private EbmlMaster muxInfo(VirtualTrack[] tracks) {
        EbmlMaster master = (EbmlMaster) createByType(Info);
        createChild(master, TimecodeScale, TIMESCALE);
        createChild(master, WritingApp, "JCodec v0.1.7");
        createChild(master, MuxingApp, "JCodec MKVStreamingMuxer v0.1.7");
        
        WebmCluster lastCluster = webmClusters.get(webmClusters.size()-1);
        createChild(master, MKVType.Duration, (lastCluster.pkt.getPts()+lastCluster.pkt.getDuration())*MULTIPLIER);
        createChild(master, DateUTC, new Date());
        return master;
    }

    private EbmlMaster muxTracks(VirtualTrack[] tracks) {
        EbmlMaster master = (EbmlMaster) createByType(Tracks);
        for (int i = 0; i < tracks.length; i++) {
            VirtualTrack track = tracks[i];
            EbmlMaster trackEntryElem = (EbmlMaster) createByType(TrackEntry);

            createChild(trackEntryElem, TrackNumber, i + 1);

            createChild(trackEntryElem, TrackUID, i + 1);
            CodecMeta codecMeta = track.getCodecMeta();
            if (VP80_FOURCC.equalsIgnoreCase(track.getCodecMeta().getFourcc())) {
                createChild(trackEntryElem, TrackType, (byte) 0x01);
                createChild(trackEntryElem, Name, "Track " + (i + 1) + " Video");
                createChild(trackEntryElem, CodecID, "V_VP8");
                createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());

                if (codecMeta instanceof VideoCodecMeta) {
                    VideoCodecMeta vcm = (VideoCodecMeta) codecMeta;
                    EbmlMaster trackVideoElem = (EbmlMaster) createByType(Video);

                    createChild(trackVideoElem, PixelWidth, vcm.getSize().getWidth());
                    createChild(trackVideoElem, PixelHeight, vcm.getSize().getHeight());

                    trackEntryElem.add(trackVideoElem);
                }
                
            } else if ("vrbs".equalsIgnoreCase(track.getCodecMeta().getFourcc())) {
                createChild(trackEntryElem, TrackType, (byte) 0x02);
                createChild(trackEntryElem, Name, "Track " + (i + 1) + " Audio");
                createChild(trackEntryElem, CodecID, "A_VORBIS");
                createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
                
                if (codecMeta instanceof AudioCodecMeta) {
                    AudioCodecMeta acm = (AudioCodecMeta) codecMeta;
                    EbmlMaster trackAudioElem = (EbmlMaster) createByType(Audio);
                    createChild(trackAudioElem, Channels, acm.getChannelCount());
                    createChild(trackAudioElem, BitDepth, acm.getSampleSize());
                    createChild(trackAudioElem, SamplingFrequency, acm.getSampleRate()); 

                    trackEntryElem.add(trackAudioElem);
                }
            }

            master.add(trackEntryElem);
        }
        return master;
    }

    private EbmlMaster muxSeekHead() {
        SeekHeadFactory shi = new SeekHeadFactory();
        shi.add(mkvInfo);
        shi.add(mkvTracks);
        shi.add(mkvCues);
        return shi.indexSeekHead();
    }
    
    private void muxCues(VirtualTrack[] tracks) {
        int trackIndex = findFirstVP8TrackIndex(tracks);
        trackIndex += 1;
        
        CuesFactory ci = new CuesFactory(mkvSeekHead.size() + mkvInfo.size() + mkvTracks.size(), trackIndex);
        for (WebmCluster aCluster : webmClusters)
            ci.add(CuesFactory.CuePointMock.make(aCluster.c));

        EbmlMaster indexedCues = ci.createCues();
        for (EbmlBase aCuePoint : indexedCues.children)
            mkvCues.add(aCuePoint);
    }
    
    private static int findFirstVP8TrackIndex(VirtualTrack[] tracks){
        for (int i=0; i<tracks.length; i++)
            if (VP80_FOURCC.equalsIgnoreCase(tracks[i].getCodecMeta().getFourcc()))
                return i;
        return -1;
    }
    
    public class WebmCluster implements MovieSegment {

        MkvBlock be = MKVType.createByType(SimpleBlock);
        EbmlMaster c = MKVType.createByType(Cluster);
        public VirtualPacket pkt;
        private int chunkNo;
        private int trackNo;
        private long previousClustersSize;

        public WebmCluster(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo, long previousClustersSize) {
            this.pkt = pkt;
            this.chunkNo = chunkNo;
            this.trackNo = trackNo+1;
            this.previousClustersSize = previousClustersSize;
            long timecode = (long) (pkt.getPts()*MULTIPLIER);
            createChild(c, Timecode, timecode); 
            
            try {
                be.frameSizes = new int[] { this.pkt.getDataLen() };
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to read size of the frame", ioe);
            }
            be.timecode = 0;
            be.trackNumber = this.trackNo;
            be.discardable = false;
            be.lacingPresent = false;
            be.dataLen = be.getDataSize();
            
            c.add(be);
        }

        @Override
        public ByteBuffer getData() throws IOException {
            be.frames = new ByteBuffer[1];
            be.frames[0] = pkt.getData().duplicate();
            ByteBuffer data = c.getData();
            Assert.assertEquals("computed and actuall cluster sizes MUST match", (int) c.size(), data.remaining());
            return data;
        }

        @Override
        public int getNo() {
            return this.chunkNo;
        }

        @Override
        public long getPos() {
            try {
                return this.previousClustersSize+headerChunk.getDataLen();
            } catch (IOException e) {
                throw new RuntimeException("Couldn't compute header length", e);
            }
        }

        @Override
        public int getDataLen() throws IOException {
            return (int) c.size();
        }

    }
    
    public static class HeaderSegment implements MovieSegment {
        
        private List<EbmlMaster> header;

        public HeaderSegment(List<EbmlMaster> header) {
            this.header = header;
        }

        @Override
        public long getPos() {
            return 0;
        }
        
        @Override
        public int getNo() {
            return 0;
        }
        
        @Override
        public int getDataLen() throws IOException {
            int size = 0;
            for (EbmlMaster m : header)
                if (Segment.equals(m.type)){
                    size += ((MkvSegment)m).getHeaderSize(); 
                } else {
                    size += m.size();
                }
            return size;
        }
        
        @Override
        public ByteBuffer getData() throws IOException {
            ByteBuffer data = ByteBuffer.allocate(getDataLen());
            for (EbmlMaster m : header)
                if (Segment.equals(m.type)){
                    data.put(((MkvSegment)m).getHeader());
                } else {
                    data.put(m.getData());
                }
            
            data.flip();
            return data;
        }
    }
}
