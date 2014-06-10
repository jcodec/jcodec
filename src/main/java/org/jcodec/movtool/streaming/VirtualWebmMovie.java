package org.jcodec.movtool.streaming;

import static org.jcodec.containers.mkv.MKVMuxer.createAndAddElement;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.containers.mkv.OnDemandMKVMuxer.MKVMuxerTrack;
import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * WebM specific muxing
 * 
 * @author The JCodec project
 * 
 */
public class VirtualWebmMovie extends VirtualMovie {
    
    List<MKVMuxerTrack> tracks = new ArrayList<MKVMuxerTrack>();
    private MasterElement mkvInfo;
    private MasterElement mkvTracks;
    private MasterElement mkvCues;
    private MasterElement mkvSeekHead;
    private MasterElement ebmlHeader;
    private MasterElement segmentElem;
    private LinkedList<Cluster> mkvClusters = new LinkedList<Cluster>();

    public VirtualWebmMovie(VirtualTrack[] tracks) throws IOException {
        super(tracks);
    }

    @Override
    protected MovieSegment packetChunk(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo, long pos) {
        return new WebmCluster(track, pkt, chunkNo, trackNo, pos);
    }

    @Override
    protected MovieSegment headerChunk(List<MovieSegment> chunks, VirtualTrack[] tracks, long dataSize)
            throws IOException {
        
        return null;
    }

    public static class WebmCluster implements MovieSegment {
        
        BlockElement be = new BlockElement(Type.SimpleBlock.id);
        Cluster c = Type.createElementByType(Type.Cluster);
        private VirtualPacket pkt;
        private int chunkNo;
        private int trackNo;
        private long pos;
        private VirtualTrack track;

        public WebmCluster(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo, long pos) {
            this.track = track;
            this.pkt = pkt;
            this.chunkNo = chunkNo;
            this.trackNo = trackNo;
            this.pos = pos;
            createAndAddElement(c, Type.Timecode, pkt.getFrameNo());
            c.timecode = pkt.getFrameNo();
        }

        @Override
        public ByteBuffer getData() throws IOException {
            return pkt.getData().duplicate();
        }

        @Override
        public int getNo() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getPos() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getDataLen() throws IOException {
            // TODO Auto-generated method stub
            return 0;
        }
        
        /**
         *      int framesPerCluster = NANOSECONDS_IN_A_SECOND/timescale;
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
         */
        
    }
}
