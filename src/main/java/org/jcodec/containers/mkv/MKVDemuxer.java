package org.jcodec.containers.mkv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.common.LongArrayList;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mkv.ebml.BinaryElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.jcodec.containers.mkv.elements.TrackEntryElement;
import org.jcodec.containers.mkv.elements.Tracks;

public class MKVDemuxer {
    
    private List<MKVDemuxer.MKVDemuxerTrack> tracks = new ArrayList<MKVDemuxer.MKVDemuxerTrack>();
    private FileChannel input;
    private UnsignedIntegerElement scale;
    
    public static MKVDemuxer getDemuxer(FileChannel fc) throws IOException{
        SimpleEBMLParser par = new SimpleEBMLParser(fc);
        par.parse();
        return new MKVDemuxer(par.getTree(), fc);
    }
    
    public static Map<Long, List<BlockElement>> demuxClusters(Cluster[] clusters){
        Map<Long, List<BlockElement>> perTrackBlocs = new HashMap<Long, List<BlockElement>>();
        for (Cluster c : clusters){
            UnsignedIntegerElement uie  = (UnsignedIntegerElement) Type.findFirst(c, Type.Cluster, Type.Timecode);
            long timecode = uie.get();
            for(Element e : c.children){
                BlockElement be = null;
                if (e instanceof BlockElement){
                    be = (BlockElement) e;
                } else if (e.type.equals(Type.BlockGroup)) { 
                    be = (BlockElement) Type.findFirst(e, Type.BlockGroup, Type.Block);
                } 
                
                if (be == null)
                    continue;
                
                be.absoluteTimecode = timecode + be.timecode;
                
                Long no = Long.valueOf(be.trackNumber);
                if (!perTrackBlocs.containsKey(no)){
                    perTrackBlocs.put(no, new ArrayList<BlockElement>());
                }
                perTrackBlocs.get(no).add(be);
            }
        }
        return perTrackBlocs;
    }

    public MKVDemuxer(List<MasterElement> tree, FileChannel input) {
        this.input = input;
        Tracks tracs = Type.findFirst(tree, Type.Segment, Type.Tracks);
        Cluster[] clusters = Type.findAll(tree, Cluster.class, Type.Segment, Type.Cluster);
        scale = Type.findFirst(tree, Type.Segment, Type.Info, Type.TimecodeScale);
        Map<Long, List<BlockElement>> p = demuxClusters(clusters);
        for(TrackEntryElement track : Type.findAll(tracs, TrackEntryElement.class, Type.Tracks, Type.TrackEntry)){
            MasterElement video = (MasterElement) Type.findFirst(track, Type.TrackEntry, Type.Video);
            MasterElement audio = (MasterElement) Type.findFirst(track, Type.TrackEntry, Type.Audio);
            UnsignedIntegerElement trackNo = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.TrackNumber);
            long no = trackNo.get();
            List<BlockElement> bes = p.get(Long.valueOf(no)); 
            MKVDemuxer.MKVDemuxerTrack t = null;
            if (video != null){
                t = new VideoTrack(track, no, bes);
            } else if (audio != null){
                t = new AudioTrack(track, no, bes);
            } else {
                t = new OtherTrack(track, no, bes);
            }
            tracks.add(t);
        }
    }

    public VideoTrack getVideoTrack() {
        for (MKVDemuxer.MKVDemuxerTrack t : tracks)
            if (t instanceof VideoTrack)
                return (VideoTrack) t;
        
        return null;
    }

    public MKVDemuxer.MKVDemuxerTrack getTrack(int no) {
        return tracks.get(no);
    }

    public List<AudioTrack> getAudioTracks() {
        List<AudioTrack> audioTracks = new ArrayList<AudioTrack>();
        for (MKVDemuxer.MKVDemuxerTrack t : tracks)
            if (t instanceof AudioTrack)
                audioTracks.add((AudioTrack)t);
        
        return audioTracks;
    }
    
    public MKVDemuxer.MKVDemuxerTrack[] getTracks() {
        return tracks.toArray(new MKVDemuxer.MKVDemuxerTrack[]{});
    }
    
    
    public static interface MKVDemuxerTrack {

        public Packet getFrames(ByteBuffer buffer, int n) throws IOException;

        public Packet getFrames(int n) throws IOException;

        public void seekPointer(int frameNo);

        public int getFrameCount();
        
        public String getCodecID();
        
        public long getNo();
        
    }
    
    public class VideoTrack implements MKVDemuxerTrack {

        private long no;
        private List<BlockElement> bes;
        private int pointer = 0;
        public long defaultDuration;
        public long pixelWidth = -1;
        public long pixelHeight = -1;
        public long displayWidth = -1;
        public long displayHeight = -1;
        public long displayUnit = 0;
        public ByteBuffer codecState;
        public String codec;

        public VideoTrack(TrackEntryElement track, long no, List<BlockElement> bes) {
            this.no = no;
            this.bes = bes;
            UnsignedIntegerElement uie = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.DefaultDuration);
            if (uie != null)
                this.defaultDuration = uie.get();

            UnsignedIntegerElement width   = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.Video, Type.PixelWidth);
            if (width != null)
                this.pixelWidth = width.get();
            
            UnsignedIntegerElement height  = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.Video, Type.PixelHeight);
            if (height != null)
                this.pixelHeight = height.get();
            
            UnsignedIntegerElement dwidth  = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.Video, Type.DisplayWidth);
            if (dwidth != null)
                this.displayWidth = dwidth.get();
            
            UnsignedIntegerElement dheight = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.Video, Type.DisplayHeight);
            if (dheight != null)
                this.displayHeight = dheight.get();
            
            UnsignedIntegerElement dunit   = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.Video, Type.DisplayUnit);
            if (dunit != null)
                this.displayUnit = dunit.get();
            
            BinaryElement cprivate = (BinaryElement) Type.findFirst(track, Type.TrackEntry, Type.CodecPrivate);
            if (cprivate != null)
                this.codecState = ByteBuffer.wrap(cprivate.getData());
            
            StringElement codecelement = (StringElement) Type.findFirst(track, Type.TrackEntry, Type.CodecID);
            if (codecelement != null)
                this.codec = codecelement.get();
        }

        public Packet getFrames(ByteBuffer buffer, int n) throws IOException {
            if (n != 1)
                throw new IllegalArgumentException("Frames should be = 1 for this track");
            
            if (pointer >= bes.size())
                return null;
            
            BlockElement block = bes.get(pointer);
            int size = (int) block.frameSizes[0];

            if (buffer.remaining() < size)
                throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
            
            Packet packet = readFrame(buffer, size, block);
            this.pointer++;
            return packet;
        }


        public Packet getFrames(int n) throws IOException {
            if (n != 1)
                throw new IllegalArgumentException("Frames should be = 1 for this track");
            
            if (pointer >= bes.size())
                return null;
            
            BlockElement be = bes.get(pointer);
            
            Packet packet = readFrame(ByteBuffer.allocate((int) be.frameSizes[0]), n, be);
            this.pointer++;
            return packet;
        }
        
        private Packet readFrame(ByteBuffer buffer, int size, BlockElement block) throws IOException {
            input.position(block.frameOffsets[0]);
            if (NIOUtils.read(input, buffer) < size)
                return null;
            
            buffer.flip();
            
            long timecale = scale != null ? scale.get() : 1L; 
            return new Packet(buffer, 0L, timecale, 0L, pointer, block.keyFrame, new TapeTimecode((short)0, (byte)0, (byte)0, (byte)0, false));
        }

        public void seekPointer(int frameNo) {
            pointer = frameNo;
        }

        public int getFrameCount() {
            return bes.size();
        }

        @Override
        public long getNo() {
            return no;
        }

        @Override
        public String getCodecID() {
            return codec;
        }
        
    }
    
    public class AudioTrack implements MKVDemuxer.MKVDemuxerTrack {

        private long no;
        long[] sampleOffsets;
        private long[] sampleSizes;
        int currentSampleNo = 0;
        private String codec;

        public AudioTrack(TrackEntryElement track, long no, List<BlockElement> bes) {
            this.no = no;
            LongArrayList offsets = new LongArrayList();
            LongArrayList sizes = new LongArrayList();
            for(BlockElement be : bes){
                offsets.addAll(be.frameOffsets);
                sizes.addAll(be.frameSizes);
            }
            
            sampleOffsets = offsets.toArray();
            sampleSizes = sizes.toArray();
            
            StringElement codecelement = (StringElement) Type.findFirst(track, Type.TrackEntry, Type.CodecID);
            if (codecelement != null)
                this.codec = codecelement.get();
        }

        public Packet getFrames(ByteBuffer buffer, int n) throws IOException {
            if (n<=0 ||  (currentSampleNo+n-1) > sampleOffsets.length)
                return null;
            
            int size = 0;
            for(int i=0;i<n;i++)
                size += sampleSizes[currentSampleNo+i];
            
            if (buffer.remaining() < size)
                throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
            
            return readFrames(buffer, size, n);
        }

        public Packet getFrames(int n) throws IOException {
            if (n<=0 || (currentSampleNo+n-1) >= sampleOffsets.length)
                return null;
            
            int size = 0;
            for(int i=0;i<n;i++)
                size += sampleSizes[currentSampleNo+i];
            
            return readFrames(ByteBuffer.allocate(size), size, n);
        }
        
        private Packet readFrames(ByteBuffer buffer, int size, int n) throws IOException {
            for (int i = 0; i < n; i++) {
                input.position(sampleOffsets[currentSampleNo + i]);
                if (NIOUtils.read(input, buffer, (int) sampleSizes[currentSampleNo + i]) < sampleSizes[currentSampleNo + i])
                    return null;
            }
            
            buffer.flip();
            
            long timecale = scale != null ? scale.get() : 1L; 
            return new Packet(buffer, 0L, timecale, 0L, currentSampleNo, true, new TapeTimecode((short)0, (byte)0, (byte)0, (byte)0, false));
        }

        public void seekPointer(int sampleNo) {
            this.currentSampleNo = sampleNo;
        }

        public int getFrameCount() {
            return sampleOffsets.length;
        }

        @Override
        public long getNo() {
            return no;
        }

        @Override
        public String getCodecID() {
            return codec;
        }
        
    }
    
    public class OtherTrack implements MKVDemuxerTrack {

        private long no;
        private List<BlockElement> bes;

        public OtherTrack(TrackEntryElement track, long no, List<BlockElement> bes) {
            this.no = no;
            this.bes = bes;
        }

        @Override
        public Packet getFrames(ByteBuffer buffer, int n) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Packet getFrames(int n) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void seekPointer(int frameNo) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getFrameCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getNo() {
            return no;
        }

        @Override
        public String getCodecID() {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}