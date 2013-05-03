package org.jcodec.containers.mkv;

import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.jcodec.containers.mkv.MKVMuxerTest.tildeExpand;
import gnu.trove.list.array.TLongArrayList;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mkv.MKVDemuxerTest.MKVDemuxer.VideoTrack;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.jcodec.containers.mkv.elements.TrackEntryElement;
import org.jcodec.containers.mkv.elements.Tracks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MKVDemuxerTest {
    MKVDemuxer dem = null;
    private FileInputStream demInputStream;
    private SimpleEBMLParser par;
    
    @Before
    public void setUp() throws IOException{
        
        FileInputStream inputStream = new FileInputStream(MKVMuxerTest.tildeExpand("./src/test/resources/mkv/10frames.webm"));
        par = new SimpleEBMLParser(inputStream .getChannel());
        try {
            par.parse();
        } finally {
            closeQuietly(inputStream);
        }
        
        demInputStream = new FileInputStream(MKVMuxerTest.tildeExpand("./src/test/resources/mkv/10frames.webm"));
        dem = new MKVDemuxer(par.getTree(), demInputStream.getChannel());
    }
    
    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

    @Test
    public void testGetFrame() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTrack());
        Assert.assertNotNull(dem.getTracks());
        
        VideoTrack video = dem.getVideoTrack();
        Packet frame = video.getFrames(1);
        
        Assert.assertNotNull(video);
        System.out.println(video.getFrameCount());
        System.out.println(video.getNo());
        byte[] vp8Frame = readFileToByteArray(tildeExpand("./src/test/resources/mkv/10frames01.vp8"));
        Assert.assertArrayEquals(vp8Frame, frame.getData().array());
    }

    @Test
    public void testPosition() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTrack());
        Assert.assertNotNull(dem.getTracks());
        
        VideoTrack video = dem.getVideoTrack();
        video.seekPointer(1);
        Packet frame = video.getFrames(1);
        
        Assert.assertNotNull(video);
        System.out.println(video.getFrameCount());
        System.out.println(video.getNo());
        
        byte[] vp8Frame = readFileToByteArray(tildeExpand("./src/test/resources/mkv/10frames02.vp8"));
        Assert.assertArrayEquals(vp8Frame, frame.getData().array());
    }
    
    public static class MKVDemuxer {
        
        private List<MKVDemuxerTrack> tracks = new ArrayList<MKVDemuxerTrack>();
        private FileChannel input;
        private UnsignedIntegerElement scale;
        
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
                MKVDemuxerTrack t = null;
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
            for (MKVDemuxerTrack t : tracks)
                if (t instanceof VideoTrack)
                    return (VideoTrack) t;
            
            return null;
        }

        public MKVDemuxerTrack getTrack(int no) {
            return tracks.get(no);
        }

        public List<AudioTrack> getAudioTracks() {
            List<AudioTrack> audioTracks = new ArrayList<AudioTrack>();
            for (MKVDemuxerTrack t : tracks)
                if (t instanceof AudioTrack)
                    audioTracks.add((AudioTrack)t);
            
            return audioTracks;
        }
        
        public MKVDemuxerTrack[] getTracks() {
            return tracks.toArray(new MKVDemuxerTrack[]{});
        }
        
        
        public static interface MKVDemuxerTrack {

            public Packet getFrames(ByteBuffer buffer, int n) throws IOException;

            public Packet getFrames(int n) throws IOException;

            public void seekPointer(int frameNo);

            public int getFrameCount();
            
            public long getNo();
            
        }
        
        public class VideoTrack implements MKVDemuxerTrack {

            private long no;
            private List<BlockElement> bes;
            private int pointer = 0;
            private long defaultDuration;

            public VideoTrack(TrackEntryElement track, long no, List<BlockElement> bes) {
                this.no = no;
                this.bes = bes;
                UnsignedIntegerElement uie = (UnsignedIntegerElement) Type.findFirst(track, Type.TrackEntry, Type.DefaultDuration);
                if (uie != null){
                    this.defaultDuration = uie.get();
                }
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
                
                return readFrame(buffer, size, block);
            }


            public Packet getFrames(int n) throws IOException {
                if (n != 1)
                    throw new IllegalArgumentException("Frames should be = 1 for this track");
                
                if (pointer >= bes.size())
                    return null;
                
                BlockElement be = bes.get(pointer);
                
                return readFrame(ByteBuffer.allocate((int) be.frameSizes[0]), n, be);
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
            
        }
        
        public class AudioTrack implements MKVDemuxerTrack {

            private long no;
            long[] sampleOffsets;
            private long[] sampleSizes;
            int currentSampleNo = 0;

            public AudioTrack(TrackEntryElement track, long no, List<BlockElement> bes) {
                this.no = no;
                TLongArrayList offsets = new TLongArrayList();
                TLongArrayList sizes = new TLongArrayList();
                for(BlockElement be : bes){
                    offsets.add(be.frameOffsets);
                    sizes.add(be.frameSizes);
                }
                
                sampleOffsets = offsets.toArray();
                sampleSizes = sizes.toArray();
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
            
        }
    }

}
