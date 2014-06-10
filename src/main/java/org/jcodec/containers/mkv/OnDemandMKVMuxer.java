package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.CuesIndexer.CuePointMock.make;
import static org.jcodec.containers.mkv.MKVMuxer.createAndAddElement;
import static org.jcodec.containers.mkv.Type.Audio;
import static org.jcodec.containers.mkv.Type.BitDepth;
import static org.jcodec.containers.mkv.Type.Channels;
import static org.jcodec.containers.mkv.Type.CodecID;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.Name;
import static org.jcodec.containers.mkv.Type.OutputSamplingFrequency;
import static org.jcodec.containers.mkv.Type.PixelHeight;
import static org.jcodec.containers.mkv.Type.PixelWidth;
import static org.jcodec.containers.mkv.Type.SamplingFrequency;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.TrackEntry;
import static org.jcodec.containers.mkv.Type.TrackNumber;
import static org.jcodec.containers.mkv.Type.TrackType;
import static org.jcodec.containers.mkv.Type.TrackUID;
import static org.jcodec.containers.mkv.Type.Tracks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mkv.MKVMuxer.TType;
import org.jcodec.containers.mkv.ebml.DateElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.FloatElement;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class OnDemandMKVMuxer {

        List<MKVMuxerTrack> tracks = new ArrayList<MKVMuxerTrack>();
        private MasterElement mkvInfo;
        private MasterElement mkvTracks;
        private MasterElement mkvCues;
        private MasterElement mkvSeekHead;
        private MasterElement ebmlHeader;
        private MasterElement segmentElem;
        private LinkedList<Cluster> mkvClusters = new LinkedList<Cluster>();
        
        public MKVMuxerTrack addVideoTrack(Size dimentions, String encoder) {
            MKVMuxerTrack video = new MKVMuxerTrack(tracks.size()+1);
            video.dimentions = dimentions;
            video.encoder = encoder;
            video.ttype = TType.VIDEO;
            tracks.add(video);
            return video;
        }

        MKVMuxerTrack addVideoTrack(Size dimentions, String encoder, int timescale) {
            MKVMuxerTrack video = new MKVMuxerTrack(tracks.size(), timescale);
            video.dimentions = dimentions;
            video.encoder = encoder;
            video.ttype = TType.VIDEO;
            tracks.add(video);
            return video;
        }

        MKVMuxerTrack addAudioTrack(Size dimentions, String encoder, int timescale, int sampleDuration, int sampleSize) {
            MKVMuxerTrack audio = new MKVMuxerTrack(tracks.size(), timescale);
            audio.encoder = encoder;
            audio.sampleDuration = sampleDuration;
            audio.sampleSize = sampleSize;
            audio.ttype = TType.AUDIO;
            tracks.add(audio);
            return audio;
        }

        void createHeaders() throws IOException {
            createEbmlHeader();

            muxSegmentHeader();
        }
        
        public ByteBuffer muxHeaders() throws IOException {
            /**
             * In order to write Cues, one has to know the sized of Clusters fist.
             * thus blocks are organized into clusters before writing header.
             * 
             */
            getVideoTrack().clusterBlocks();
            
            // EBML
            // SeekHead
            // Info
            // Tracks
            // Cues
            createHeaders();
            // Clusters
            addClusters();

            long ebmlHeaderSize = ebmlHeader.getSize();
            ebmlHeaderSize += ebmlHeader.id.length + Element.ebmlBytes(ebmlHeaderSize).length;
            
            int segmentHeaderSize = (int) conditionalDataSize(segmentElem);
            segmentHeaderSize += segmentElem.id.length + Element.ebmlBytes(segmentElem.getSize()).length;
            ByteBuffer bb = ByteBuffer.allocate((int)(ebmlHeaderSize+segmentHeaderSize));
            conditionalMux(bb, ebmlHeader);
            conditionalMux(bb, segmentElem);
            
            // TODO: Chapters
            // TODO: Attachments
            // TODO: Tags
            
            bb.flip();
            return bb;
        }
        
        private long conditionalMux(ByteBuffer os, MasterElement el) throws IOException {
            long size = conditionalDataSize(el);
            byte[] ebmledSize = Element.ebmlBytes(size);
            os.put(el.id);
            os.put(ebmledSize);
            
            
            for (int i = 0; i < el.children.size(); i++) {
                Element element = el.children.get(i);
                if (!(element instanceof Cluster))
                    os.put(element.mux());
            }
            return os.position();
        }
        
        private long conditionalDataSize(MasterElement el){
            long returnValue = 0;
            if (el.children != null && !el.children.isEmpty()){
                // Either account for all the children
                for(Element e : el.children)
                    if (!(e instanceof Cluster))
                        returnValue += e.getSize(); 
            } else {
                // Or just rely on size attribute if no children are present
                //    this happens while reading the file
                returnValue += el.size;
            }
            return returnValue;
        }


        private void muxSegmentHeader() {
            // # Segment
            segmentElem = (MasterElement) Type.createElementByType(Segment);

            // # Meta Seek
            // muxSeeks(segmentElem);
            muxInfo();
            muxTracks();
            muxSeekHead();
            muxCues();


            // Tracks Info
            segmentElem.addChildElement(mkvSeekHead);
            segmentElem.addChildElement(mkvInfo);
            segmentElem.addChildElement(mkvTracks);
            segmentElem.addChildElement(mkvCues);
        }

        private void muxCues() {
            CuesIndexer ci = new CuesIndexer(cuesOffsset(), 1);
            for (Cluster aCluster : mkvClusters)
                ci.add(make(aCluster));

            MasterElement indexedCues = ci.createCues();
            for (Element aCuePoint : indexedCues.children)
                mkvCues.addChildElement(aCuePoint);

            System.out.println("cues size: " + mkvCues.getSize());
        }

        private void muxSeekHead() {
            SeekHeadIndexer shi = new SeekHeadIndexer();
            mkvCues = (MasterElement) Type.createElementByType(Cues);
            shi.add(mkvInfo);
            shi.add(mkvTracks);
            shi.add(mkvCues);
            mkvSeekHead = shi.indexSeekHead();
        }

        private long cuesOffsset() {
            return mkvSeekHead.getSize() + mkvInfo.getSize() + mkvTracks.getSize();
        }

        private void createEbmlHeader() throws IOException {
            ebmlHeader = (MasterElement) Type.createElementByType(Type.EBML);

            StringElement docTypeElem = (StringElement) Type.createElementByType(Type.DocType);
            docTypeElem.set("matroska");

            UnsignedIntegerElement docTypeVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeVersion);
            docTypeVersionElem.set(2);

            UnsignedIntegerElement docTypeReadVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeReadVersion);
            docTypeReadVersionElem.set(2);

            ebmlHeader.addChildElement(docTypeElem);
            ebmlHeader.addChildElement(docTypeVersionElem);
            ebmlHeader.addChildElement(docTypeReadVersionElem);
        }

        private void addClusters() {
            for (Cluster cluster : mkvClusters) 
                segmentElem.addChildElement(cluster);
        }

        private void muxTracks() {
            mkvTracks = (MasterElement) Type.createElementByType(Tracks);
            for (MKVMuxerTrack track : tracks) {
                MasterElement trackEntryElem = (MasterElement) Type.createElementByType(TrackEntry);

                createAndAddElement(trackEntryElem, TrackNumber, track.trackId);

                createAndAddElement(trackEntryElem, TrackUID, track.trackId);

                createAndAddElement(trackEntryElem, TrackType, track.getMkvType());

                createAndAddElement(trackEntryElem, Name, track.getName());

//                trackEntryElem.addChildElement(findFirst(track, TrackEntry, Language));

                createAndAddElement(trackEntryElem, CodecID, track.encoder);

//                trackEntryElem.addChildElement(findFirst(track, TrackEntry, CodecPrivate));

//                trackEntryElem.addChildElement(findFirst(track, TrackEntry, DefaultDuration));

                // Now we add the audio/video dependant sub-elements
                if (track.isVideo()) {
                    MasterElement trackVideoElem = (MasterElement) Type.createElementByType(Type.Video);

                    createAndAddElement(trackVideoElem, PixelWidth, track.dimentions.getWidth());
                    createAndAddElement(trackVideoElem, PixelHeight, track.dimentions.getHeight());

                    trackEntryElem.addChildElement(trackVideoElem);
                } else if (track.isAudio()) {
                    MasterElement trackAudioElem = (MasterElement) Type.createElementByType(Audio);

                    createAndAddElement(trackAudioElem, Channels, track.channels);
                    createAndAddElement(trackAudioElem, BitDepth, track.bitdepth);
                    createAndAddElement(trackAudioElem, SamplingFrequency, track.samplingFrequency);
                    createAndAddElement(trackAudioElem, OutputSamplingFrequency, track.outputSamplingFrequency);

                    trackEntryElem.addChildElement(trackAudioElem);
                }

                mkvTracks.addChildElement(trackEntryElem);
            }
        }

        private void muxInfo() {
            // # Segment Info
            mkvInfo = (MasterElement) Type.createElementByType(Type.Info);

            // Add timecode scale
            UnsignedIntegerElement timecodescaleElem = (UnsignedIntegerElement) Type.createElementByType(Type.TimecodeScale);
            timecodescaleElem.set(getVideoTrack().getTimescale());
            mkvInfo.addChildElement(timecodescaleElem);

            FloatElement durationElem = (FloatElement) Type.createElementByType(Type.Duration);
            durationElem.set(getVideoTrack().getTrackTotalDuration());
            mkvInfo.addChildElement(durationElem);

            DateElement dateElem = (DateElement) Type.createElementByType(Type.DateUTC);
            dateElem.setDate(new Date());
            mkvInfo.addChildElement(dateElem);

            StringElement writingAppElem = (StringElement) Type.createElementByType(Type.WritingApp);
            writingAppElem.set("JCodec v0.1.0");
            mkvInfo.addChildElement(writingAppElem);

            StringElement muxingAppElem = (StringElement) Type.createElementByType(Type.MuxingApp);
            muxingAppElem.set("JCodec MKVMuxer v0.1a");
            mkvInfo.addChildElement(muxingAppElem);
        }

        MKVMuxerTrack getVideoTrack() {
            for (MKVMuxerTrack track : tracks)
                if (track.isVideo())
                    return track;
            return null;
        }

        MKVMuxerTrack getTimecodeTrack() {
            for (MKVMuxerTrack track : tracks)
                if (track.isTimecode())
                    return track;
            return null;
        }

        List<MKVMuxerTrack> getAudioTracks() {
            List<MKVMuxerTrack> audio = new ArrayList<MKVMuxerTrack>();
            for (MKVMuxerTrack t : tracks)
                if (t.isAudio())
                    audio.add(t);

            return audio;
        }

        // MuxerTrack
        // UncompressedTrack
        // TimecodeTrack
        // CompressedTrack
        public class MKVMuxerTrack {
            
            private static final int NANOSECONDS_IN_A_SECOND = 1000000000;
            public int bitdepth;
            public int channels;
            public double outputSamplingFrequency;
            public double samplingFrequency;
            public int sampleSize;
            public int sampleDuration;
            public long chunkDuration;
            public String encoder;
            public Size dimentions;
            public int trackId;
            private int timescale = 40000000;
            public int currentBlock = 0;
            public List<BlockElement> blocks = new ArrayList<BlockElement>();
            MKVMuxer.TType ttype = TType.VIDEO;

            
            MKVMuxerTrack(int trackId) {
                this.trackId = trackId;
            }

            MKVMuxerTrack(int trackId, int timescale) {
                this.trackId = trackId;
                this.timescale = timescale;
            }

            public String getName() {
                String name = "";
                if (isVideo())
                    name = "Video";
                if (isAudio())
                    name = "Audio";
                
                return name+trackId;
            }

            public byte getMkvType() {
                //A set of track types coded on 8 bits (1: video, 2: audio, 3: complex, 0x10: logo, 0x11: subtitle, 0x12: buttons, 0x20: control).
                if (isVideo())
                    return 0x01;
                if (isAudio())
                    return 0x02;
                return 0x03;
            }

            public void setTgtChunkDuration(Rational duration, Unit unit) {

            }

            long getTrackTotalDuration() {
                return 0;
            }

            int getTimescale() {
                return timescale;
            }

            boolean isVideo() {
                return TType.VIDEO.equals(this.ttype);
            }

            boolean isTimecode() {
                return false;
            }

            boolean isAudio() {
                return TType.AUDIO.equals(this.ttype);
            }

            Size getDisplayDimensions() {
                return null;
            }

            public void addSampleEntry(BlockElement se) {
                blocks.add(se);
            }
            
            public void clusterBlocks() {
                int framesPerCluster = NANOSECONDS_IN_A_SECOND/timescale;
                long i=0;
                for (BlockElement be : blocks){
                    if (i%framesPerCluster == 0) {
                        Cluster c = Type.createElementByType(Type.Cluster);
                        createAndAddElement(c, Type.Timecode, i);
                        c.timecode = i;
                        
                        if (!mkvClusters.isEmpty()){
                            long prevSize = mkvClusters.getLast().getSize();
                            createAndAddElement(c, Type.PrevSize, prevSize);
                            c.prevsize = prevSize;
                        }
                        
                        mkvClusters.add(c);
                    }
                    Cluster c = mkvClusters.getLast();
                    be.timecode = (int)(i - c.timecode);
                    c.addChildElement(be);
                    i++;
                }
            }

        }
    }